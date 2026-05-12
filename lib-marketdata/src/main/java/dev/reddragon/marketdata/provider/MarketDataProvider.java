package dev.reddragon.marketdata.provider;

import dev.reddragon.marketdata.model.MarketBar;

import java.time.LocalDate;
import java.util.List;

/**
 * Broker/data-provider abstraction for historical market bars.
 */
public interface MarketDataProvider {

    /**
     * Return daily historical bars for {@code symbol} in the inclusive date range.
     */
    List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to);
}
