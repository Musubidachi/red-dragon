package dev.reddragon.ingestion.services.sec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.ingestion.config.SecApiProperties;

class CikLookupServiceTest {

    private static final String COMPANY_TICKERS_URL = "https://www.sec.gov/files/company_tickers.json";
    private static final URI COMPANY_TICKERS_URI = URI.create(COMPANY_TICKERS_URL);

    @Test
    void loadsTickerMapOnceWithinTtl() {
        SecHttpClient httpClient = mock(SecHttpClient.class);
        when(httpClient.process(COMPANY_TICKERS_URI)).thenReturn(companyJson("AAPL", "320193"));
        MutableClock clock = new MutableClock(Instant.parse("2026-05-26T12:00:00Z"));
        CikLookupService service = service(httpClient, clock, 1_000L);

        assertEquals(Optional.of("0000320193"), service.process(" aapl "));
        assertEquals(Optional.of("0000320193"), service.process("AAPL"));

        verify(httpClient, times(1)).process(COMPANY_TICKERS_URI);
    }

    @Test
    void refreshesTickerMapAfterTtlExpires() {
        SecHttpClient httpClient = mock(SecHttpClient.class);
        when(httpClient.process(COMPANY_TICKERS_URI))
                .thenReturn(companyJson("ACME", "1"))
                .thenReturn(companyJson("ACME", "2"));
        MutableClock clock = new MutableClock(Instant.parse("2026-05-26T12:00:00Z"));
        CikLookupService service = service(httpClient, clock, 1_000L);

        assertEquals(Optional.of("0000000001"), service.process("ACME"));
        clock.advance(Duration.ofMillis(999));
        assertEquals(Optional.of("0000000001"), service.process("ACME"));
        clock.advance(Duration.ofMillis(1));
        assertEquals(Optional.of("0000000002"), service.process("ACME"));

        verify(httpClient, times(2)).process(COMPANY_TICKERS_URI);
    }

    @Test
    void schedulesSuccessfulRefreshFromCompletionTime() {
        SecHttpClient httpClient = mock(SecHttpClient.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-05-26T12:00:00Z"));
        when(httpClient.process(COMPANY_TICKERS_URI))
                .thenAnswer(invocation -> {
                    clock.advance(Duration.ofMillis(1_000));
                    return companyJson("AAPL", "320193");
                })
                .thenReturn(companyJson("AAPL", "320194"));
        CikLookupService service = service(httpClient, clock, 1_000L);

        assertEquals(Optional.of("0000320193"), service.process("AAPL"));
        assertEquals(Optional.of("0000320193"), service.process("AAPL"));

        verify(httpClient, times(1)).process(COMPANY_TICKERS_URI);
    }

    @Test
    void refreshFailureUsesCachedMapUntilNextTtlWindow() {
        SecHttpClient httpClient = mock(SecHttpClient.class);
        when(httpClient.process(COMPANY_TICKERS_URI))
                .thenReturn(companyJson("AAPL", "320193"))
                .thenThrow(new IllegalStateException("SEC unavailable"));
        MutableClock clock = new MutableClock(Instant.parse("2026-05-26T12:00:00Z"));
        CikLookupService service = service(httpClient, clock, 1_000L);

        assertEquals(Optional.of("0000320193"), service.process("AAPL"));
        clock.advance(Duration.ofMillis(1_000));
        assertEquals(Optional.of("0000320193"), service.process("AAPL"));
        assertEquals(Optional.of("0000320193"), service.process("AAPL"));

        verify(httpClient, times(2)).process(COMPANY_TICKERS_URI);
    }

    @Test
    void initialLoadFailureStillFailsClosed() {
        SecHttpClient httpClient = mock(SecHttpClient.class);
        when(httpClient.process(COMPANY_TICKERS_URI)).thenThrow(new IllegalStateException("SEC unavailable"));
        MutableClock clock = new MutableClock(Instant.parse("2026-05-26T12:00:00Z"));
        CikLookupService service = service(httpClient, clock, 1_000L);

        assertThrows(IllegalStateException.class, () -> service.process("AAPL"));
    }

    private CikLookupService service(SecHttpClient httpClient, Clock clock, long ttlMillis) {
        return new CikLookupService(properties(ttlMillis), httpClient, new ObjectMapper(), clock);
    }

    private SecApiProperties properties(long ttlMillis) {
        return new SecApiProperties(
                "red-dragon (ops@reddragon.dev)",
                "https://data.sec.gov/submissions/",
                COMPANY_TICKERS_URL,
                10,
                SecApiProperties.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                SecApiProperties.DEFAULT_READ_TIMEOUT_MILLIS,
                SecApiProperties.DEFAULT_MAX_RETRIES,
                SecApiProperties.DEFAULT_BACKOFF_BASE_MILLIS,
                SecApiProperties.DEFAULT_MAX_BACKOFF_MILLIS,
                ttlMillis
        );
    }

    private String companyJson(String ticker, String cik) {
        return """
                {
                  "0": {
                    "cik_str": %s,
                    "ticker": "%s",
                    "title": "%s Inc."
                  }
                }
                """.formatted(cik, ticker, ticker);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant) {
            this(instant, ZoneId.of("UTC"));
        }

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
