package dev.reddragon.marketdata.services.provider;

import dev.reddragon.marketdata.models.MarketBar;

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
}
