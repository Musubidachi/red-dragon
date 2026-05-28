package dev.reddragon.marketdata.services.provider.yahoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketQuote;
import dev.reddragon.marketdata.config.YahooMarketDataProperties;

class YahooMarketDataProviderTest {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";

    private RestClient restClient;
    private MockRestServiceServer server;
    private YahooMarketDataProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        provider = new YahooMarketDataProvider(enabledProperties(), restClient, new ObjectMapper());
    }

    @Test
    void disabledProviderShortCircuitsWithoutHttpCall() {
        provider = new YahooMarketDataProvider(disabledProperties(), restClient, new ObjectMapper());

        List<MarketBar> dailyBars = provider.historicalDailyBars(
                "MSFT",
                LocalDate.parse("2026-05-08"),
                LocalDate.parse("2026-05-09")
        );
        List<IntradayBar> intradayBars = provider.intradayBars(
                "MSFT",
                Instant.parse("2026-05-08T13:30:00Z"),
                Instant.parse("2026-05-08T14:30:00Z"),
                Duration.ofMinutes(5)
        );
        MarketQuote quote = provider.quote("MSFT");

        assertTrue(dailyBars.isEmpty());
        assertTrue(intradayBars.isEmpty());
        assertFalse(quote.available());
        server.verify();
    }

    @Test
    void historicalDailyBarsAreMappedSortedAndFiltered() {
        String json = chartJson(
                epoch("2026-05-09T00:00:00Z") + "," + epoch("2026-05-08T00:00:00Z") + "," + epoch("2026-05-10T00:00:00Z"),
                "110.0,100.0,0.0",
                "112.0,101.0,121.0",
                "108.0,99.0,119.0",
                "111.0,100.5,120.0",
                "900,1000,1100",
                "0.0"
        );

        server.expect(method(GET))
                .andExpect(request -> {
                    assertEquals("/v8/finance/chart/MSFT", request.getURI().getRawPath());
                    assertTrue(request.getURI().getRawQuery().contains("interval=1d"));
                })
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<MarketBar> bars = provider.historicalDailyBars(
                " msft ",
                LocalDate.parse("2026-05-08"),
                LocalDate.parse("2026-05-10")
        );

        assertEquals(2, bars.size());
        assertEquals(LocalDate.parse("2026-05-08"), bars.get(0).date());
        assertEquals(LocalDate.parse("2026-05-09"), bars.get(1).date());
        assertEquals("MSFT", bars.get(0).symbol());
        assertEquals(100.5, bars.get(0).close(), 1e-9);
        assertEquals(900L, bars.get(1).volume());
        server.verify();
    }

    @Test
    void intradayBarsMapIntervalAndTypicalPrice() {
        String json = chartJson(
                epoch("2026-05-08T13:30:00Z") + "",
                "100.0",
                "102.0",
                "98.0",
                "101.0",
                "1200",
                "0.0"
        );

        server.expect(method(GET))
                .andExpect(request -> assertTrue(request.getURI().getRawQuery().contains("interval=15m")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<IntradayBar> bars = provider.intradayBars(
                "MSFT",
                Instant.parse("2026-05-08T13:30:00Z"),
                Instant.parse("2026-05-08T14:30:00Z"),
                Duration.ofMinutes(7)
        );

        assertEquals(1, bars.size());
        assertEquals(Instant.parse("2026-05-08T13:30:00Z"), bars.get(0).startTime());
        assertEquals((102.0 + 98.0 + 101.0) / 3.0, bars.get(0).vwap(), 1e-9);
        assertEquals(1200L, bars.get(0).volume());
        server.verify();
    }

    @Test
    void quoteUsesRegularMarketPriceAndLastUsableVolume() {
        String json = chartJson(
                epoch("2026-05-07T00:00:00Z") + "," + epoch("2026-05-08T00:00:00Z"),
                "100.0,101.0",
                "102.0,103.0",
                "98.0,99.0",
                "100.5,101.5",
                "500,700",
                "123.45"
        );

        server.expect(method(GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        MarketQuote quote = provider.quote("MSFT");

        assertTrue(quote.available());
        assertEquals("MSFT", quote.symbol());
        assertEquals(123.45, quote.lastPrice(), 1e-9);
        assertEquals(700L, quote.volume());
        assertEquals(Instant.parse("2026-05-08T00:00:00Z"), quote.observedAt());
        assertEquals(MarketDataQuality.COMPLETE, quote.quality());
        server.verify();
    }

    @Test
    void quoteReportsEmptyBarsWhenChartHasNoUsablePrice() {
        String json = chartJson(
                epoch("2026-05-07T00:00:00Z") + "," + epoch("2026-05-08T00:00:00Z"),
                "100.0,101.0",
                "102.0,103.0",
                "98.0,99.0",
                "null,0.0",
                "500,700",
                "0.0"
        );

        server.expect(method(GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        MarketQuote quote = provider.quote("MSFT");

        assertFalse(quote.available());
        assertEquals(MarketDataQuality.EMPTY_BARS, quote.quality());
        assertEquals(0.0, quote.lastPrice(), 1e-9);
        assertEquals(0L, quote.volume());
        assertTrue(quote.notes().contains("Yahoo chart did not include a usable price."));
        server.verify();
    }

    @Test
    void serverErrorRetriesAndThenThrows() {
        server.expect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> provider.historicalDailyBars(
                "MSFT",
                LocalDate.parse("2026-05-08"),
                LocalDate.parse("2026-05-09")
        ));

        assertTrue(error.getMessage().contains("after retries"));
        server.verify();
    }

    private static YahooMarketDataProperties enabledProperties() {
        return new YahooMarketDataProperties(BASE_URL, true, 0L, 1, 0L);
    }

    private static YahooMarketDataProperties disabledProperties() {
        return new YahooMarketDataProperties(BASE_URL, false, 0L, 0, 0L);
    }

    private static long epoch(String instant) {
        return Instant.parse(instant).getEpochSecond();
    }

    private static String chartJson(
            String timestamps,
            String opens,
            String highs,
            String lows,
            String closes,
            String volumes,
            String regularMarketPrice
    ) {
        return "{\"chart\":{\"result\":[{\"meta\":{\"regularMarketPrice\":" + regularMarketPrice
                + "},\"timestamp\":[" + timestamps + "],\"indicators\":{\"quote\":[{\"open\":["
                + opens + "],\"high\":[" + highs + "],\"low\":[" + lows + "],\"close\":["
                + closes + "],\"volume\":[" + volumes + "]}]}}]}}";
    }
}
