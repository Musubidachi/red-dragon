package dev.reddragon.marketdata.services.provider;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Safe default provider used when a broker/data-provider token is not configured.
 */
public class NoopMarketDataProvider implements MarketDataProvider {

    @Override
    public List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to) {
        return List.of();
    }

    @Override
    public List<IntradayBar> intradayBars(String symbol, Instant from, Instant to, Duration interval) {
        return List.of();
    }

    @Override
    public MarketQuote quote(String symbol) {
        return MarketQuote.unavailable(symbol, "No market-data provider is configured.");
    }

    @Override
    public String providerName() {
        return "noop";
    }
}
