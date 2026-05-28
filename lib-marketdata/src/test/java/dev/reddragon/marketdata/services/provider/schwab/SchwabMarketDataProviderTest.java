package dev.reddragon.marketdata.services.provider.schwab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketQuote;
import dev.reddragon.marketdata.config.SchwabMarketDataProperties;

/**
 * Tests for {@link SchwabMarketDataProvider} — the longest file in the
 * lib-marketdata module previously had zero direct tests (REVIEW.md
 * Finding #14). Coverage areas matched to the README's "Testing
 * Expectations":
 *
 * <ul>
 *   <li>Happy-path historical bars: response mapping, Authorization
 *       header, chronological sort.</li>
 *   <li>Happy-path quote: lastPrice / mark / closePrice fallback chain.</li>
 *   <li>Quote with no usable price → returns unavailable (lib-marketdata
 *       Finding #13).</li>
 *   <li>Empty candles array → returns empty list, no exception.</li>
 *   <li>Malformed JSON → wrapped {@link IllegalStateException}.</li>
 *   <li>5xx server error → exhausts retries then wraps the failure.</li>
 *   <li>Disabled provider → short-circuits without an HTTP call.</li>
 * </ul>
 *
 * <p>{@link MockRestServiceServer} binds to the {@link RestClient}
 * builder so we don't need a running HTTP server; each test programs
 * the expected request/response pair, then asserts the provider's
 * behaviour. {@code verify()} catches stray or missing calls.
 */
class SchwabMarketDataProviderTest {

    private static final String BASE_URL = "https://api.schwabapi.com/marketdata/v1";
    private static final String STATIC_TOKEN = "test-token-abc";

    private RestClient restClient;
    private MockRestServiceServer server;
    private SchwabMarketDataProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        SchwabMarketDataProperties properties = enabledProperties();
        provider = new SchwabMarketDataProvider(properties, restClient, new ObjectMapper());
    }

    @Test
    void disabledProviderShortCircuitsWithoutHttpCall() {
        // Reconstruct with disabled properties; no expectations recorded =
        // any HTTP call would fail the test.
        SchwabMarketDataProperties disabled = disabledProperties();
        provider = new SchwabMarketDataProvider(disabled, restClient, new ObjectMapper());

        List<MarketBar> bars = provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-10"));

        assertTrue(bars.isEmpty(), "disabled provider must not call HTTP");
        server.verify(); // no expectations → succeeds only if no requests fired
    }

    @Test
    void happyPathHistoricalBarsAreMappedAndSorted() {
        // SEC's pricehistory returns candles in arbitrary order; provider
        // must return them sorted by date ascending.
        String json = "{\"candles\":["
                + "{\"datetime\":1746748800000,\"open\":150.0,\"high\":152.0,\"low\":149.5,\"close\":151.0,\"volume\":1000000},"
                + "{\"datetime\":1746662400000,\"open\":149.0,\"high\":150.5,\"low\":148.0,\"close\":150.0,\"volume\":900000}"
                + "],\"symbol\":\"AAPL\",\"empty\":false}";

        server.expect(method(GET))
                .andExpect(request -> assertEquals("Bearer " + STATIC_TOKEN,
                        request.getHeaders().getFirst("Authorization")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<MarketBar> bars = provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-08"), LocalDate.parse("2026-05-09"));

        assertEquals(2, bars.size());
        assertTrue(bars.get(0).date().isBefore(bars.get(1).date()),
                "bars must be sorted ascending by date");
        assertEquals("AAPL", bars.get(0).symbol());
        assertEquals(151.0, bars.get(1).close(), 1e-9);
        server.verify();
    }

    @Test
    void emptyCandlesArrayProducesEmptyList() {
        String json = "{\"candles\":[],\"symbol\":\"AAPL\",\"empty\":true}";

        server.expect(method(GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<MarketBar> bars = provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-02"));

        assertTrue(bars.isEmpty());
        server.verify();
    }

    @Test
    void malformedJsonWrapsAsIllegalStateException() {
        // Garbage body. The fetchPriceHistory branch wraps it; the
        // retry policy should classify it as non-retryable.
        server.expect(method(GET))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-02")));
        server.verify();
    }

    @Test
    void serverErrorRetriesAndReturnsSuccessfulSecondAttempt() {
        server.expect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(method(GET))
                .andRespond(withSuccess(singleCandleJson(), MediaType.APPLICATION_JSON));

        List<MarketBar> bars = provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-02"));

        assertEquals(1, bars.size());
        server.verify();
    }

    @Test
    void networkErrorRetriesAndReturnsSuccessfulSecondAttempt() {
        server.expect(method(GET))
                .andRespond(withException(new IOException("connection reset")));
        server.expect(method(GET))
                .andRespond(withSuccess(singleCandleJson(), MediaType.APPLICATION_JSON));

        List<MarketBar> bars = provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-02"));

        assertEquals(1, bars.size());
        server.verify();
    }

    @Test
    void serverErrorExhaustsRetriesAndThrows() {
        server.expect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(IllegalStateException.class, () -> provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-02")));
        server.verify();
    }

    @Test
    void rateLimitedResponseRetriesAndReturnsSuccessfulSecondAttempt() {
        server.expect(method(GET))
                .andRespond(withRawStatus(429));
        server.expect(method(GET))
                .andRespond(withSuccess(singleCandleJson(), MediaType.APPLICATION_JSON));

        List<MarketBar> bars = provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-02"));

        assertEquals(1, bars.size());
        server.verify();
    }

    @Test
    void nonRateLimitedClientErrorIsNotRetried() {
        server.expect(method(GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(IllegalStateException.class, () -> provider.historicalDailyBars(
                "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-02")));
        server.verify();
    }

    @Test
    void quoteHappyPathReadsLastPriceMarkOrClosePriceInOrder() {
        // Schwab's quote payload nests under <symbol>.quote.* — and the
        // provider prefers lastPrice, then mark, then closePrice.
        String json = "{\"AAPL\":{\"quote\":{"
                + "\"lastPrice\":151.23,"
                + "\"bidPrice\":151.20,"
                + "\"askPrice\":151.25,"
                + "\"totalVolume\":12345,"
                + "\"quoteTimeInLong\":1746748800000"
                + "}}}";
        server.expect(method(GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        MarketQuote quote = provider.quote("AAPL");

        assertTrue(quote.available());
        assertEquals("AAPL", quote.symbol());
        assertEquals(151.23, quote.lastPrice(), 1e-9);
        assertEquals(151.20, quote.bidPrice(), 1e-9);
        assertEquals(MarketDataQuality.COMPLETE, quote.quality());
        server.verify();
    }

    @Test
    void quoteFallsBackToMarkWhenLastPriceMissing() {
        String json = "{\"AAPL\":{\"quote\":{"
                + "\"mark\":151.50,"
                + "\"bidPrice\":151.40,"
                + "\"askPrice\":151.60,"
                + "\"totalVolume\":555,"
                + "\"quoteTimeInLong\":1746748800000"
                + "}}}";
        server.expect(method(GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        MarketQuote quote = provider.quote("AAPL");

        assertEquals(151.50, quote.lastPrice(), 1e-9);
    }

    @Test
    void quoteReturnsUnavailableWhenNoPriceFieldIsPositive() {
        // Schwab "successful" response with all-zero prices (lib-marketdata
        // Finding #13). Provider must surface unavailable so the composite
        // chain falls through to the next provider.
        String json = "{\"AAPL\":{\"quote\":{"
                + "\"lastPrice\":0,"
                + "\"mark\":0,"
                + "\"closePrice\":0"
                + "}}}";
        server.expect(method(GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        MarketQuote quote = provider.quote("AAPL");

        assertFalse(quote.available(),
                "provider must surface unavailable when all price fields are zero");
    }

    @Test
    void unconfiguredQuoteReturnsUnavailableWithoutHttpCall() {
        provider = new SchwabMarketDataProvider(
                disabledProperties(), restClient, new ObjectMapper());

        MarketQuote quote = provider.quote("AAPL");

        assertFalse(quote.available());
        server.verify(); // no expectations recorded
    }

    @Test
    void blankSymbolIsRejectedAtTheBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> provider.historicalDailyBars("  ", LocalDate.now(), LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> provider.quote(""));
        server.verify(); // no HTTP calls fired
    }

    // ---- Fixtures ---------------------------------------------------------

    private static SchwabMarketDataProperties enabledProperties() {
        return new SchwabMarketDataProperties(
                BASE_URL, STATIC_TOKEN,
                /* enabled */ true,
                /* cacheTtlSeconds */ 0L,    // disable cache for tests
                /* maxRetries */ 1,           // 2 total attempts
                /* retryBackoffMillis */ 0L); // no sleeping in tests
    }

    private static SchwabMarketDataProperties disabledProperties() {
        return new SchwabMarketDataProperties(BASE_URL, STATIC_TOKEN, false, 0L, 0, 0L);
    }

    private static String singleCandleJson() {
        return "{\"candles\":["
                + "{\"datetime\":1746057600000,\"open\":150.0,\"high\":152.0,"
                + "\"low\":149.5,\"close\":151.0,\"volume\":1000000}"
                + "],\"symbol\":\"AAPL\",\"empty\":false}";
    }
}
