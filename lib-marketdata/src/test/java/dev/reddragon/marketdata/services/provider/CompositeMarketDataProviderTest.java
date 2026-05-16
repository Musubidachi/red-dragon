package dev.reddragon.marketdata.services.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketQuote;

class CompositeMarketDataProviderTest {

    @Test
    void dailyBarsFallBackToNextProvider() {
        MarketBar bar = new MarketBar("MSFT", LocalDate.of(2026, 5, 15), 10, 11, 9, 10.5, 100);
        CompositeMarketDataProvider provider = new CompositeMarketDataProvider(List.of(
                new ThrowingProvider(),
                new FixedProvider(List.of(bar), List.of(), unavailableQuote())
        ));

        List<MarketBar> bars = provider.historicalDailyBars("MSFT", LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(1, bars.size());
        assertEquals("MSFT", bars.get(0).symbol());
    }

    @Test
    void intradayBarsFallBackToNextProvider() {
        IntradayBar bar = new IntradayBar("MSFT", Instant.parse("2026-05-15T14:30:00Z"), 10, 11, 9, 10.5, 100, 10.2);
        CompositeMarketDataProvider provider = new CompositeMarketDataProvider(List.of(
                new FixedProvider(List.of(), List.of(), unavailableQuote()),
                new FixedProvider(List.of(), List.of(bar), unavailableQuote())
        ));

        List<IntradayBar> bars = provider.intradayBars("MSFT", Instant.now().minus(Duration.ofHours(1)), Instant.now(), Duration.ofMinutes(5));

        assertEquals(1, bars.size());
        assertEquals("MSFT", bars.get(0).symbol());
    }

    @Test
    void quoteFallsBackUntilAvailable() {
        MarketQuote quote = new MarketQuote("MSFT", Instant.now(), 420.0, 419.9, 420.1, 1_000_000, MarketDataQuality.COMPLETE, List.of());
        CompositeMarketDataProvider provider = new CompositeMarketDataProvider(List.of(
                new FixedProvider(List.of(), List.of(), unavailableQuote()),
                new FixedProvider(List.of(), List.of(), quote)
        ));

        MarketQuote result = provider.quote("MSFT");

        assertTrue(result.available());
        assertEquals(420.0, result.lastPrice());
    }

    private MarketQuote unavailableQuote() {
        return MarketQuote.unavailable("MSFT", "missing");
    }

    private static class FixedProvider implements MarketDataProvider {
        private final List<MarketBar> dailyBars;
        private final List<IntradayBar> intradayBars;
        private final MarketQuote quote;

        FixedProvider(List<MarketBar> dailyBars, List<IntradayBar> intradayBars, MarketQuote quote) {
            this.dailyBars = dailyBars;
            this.intradayBars = intradayBars;
            this.quote = quote;
        }

        @Override
        public List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to) {
            return dailyBars;
        }

        @Override
        public List<IntradayBar> intradayBars(String symbol, Instant from, Instant to, Duration interval) {
            return intradayBars;
        }

        @Override
        public MarketQuote quote(String symbol) {
            return quote;
        }
    }

    private static class ThrowingProvider implements MarketDataProvider {
        @Override
        public List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to) {
            throw new IllegalStateException("boom");
        }
    }
}
