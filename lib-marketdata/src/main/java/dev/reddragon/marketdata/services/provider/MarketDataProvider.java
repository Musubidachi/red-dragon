package dev.reddragon.marketdata.services.provider;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Broker/data-provider abstraction for market data.
 */
public interface MarketDataProvider {

    /**
     * Return daily historical bars for {@code symbol} in the inclusive date range.
     */
    List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to);

    /**
     * Return intraday bars for {@code symbol} in the inclusive instant range.
     */
    default List<IntradayBar> intradayBars(String symbol, Instant from, Instant to, Duration interval) {
        return List.of();
    }

    /**
     * Return the latest quote for {@code symbol}, or an unavailable quote when
     * the provider cannot serve live quote data.
     */
    default MarketQuote quote(String symbol) {
        return MarketQuote.unavailable(symbol, providerName() + " does not provide live quotes.");
    }

    /**
     * Human-readable provider name used in fallbacks and diagnostics.
     */
    default String providerName() {
        return getClass().getSimpleName();
    }
}
