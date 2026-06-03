package dev.reddragon.marketdata.services.provider;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;

class ThrowingMarketDataProvider implements MarketDataProvider {
    @Override
    public List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to) {
        throw new IllegalStateException("boom");
    }

    @Override
    public List<IntradayBar> intradayBars(String symbol, Instant from, Instant to, Duration interval) {
        throw new IllegalStateException("boom");
    }

    @Override
    public MarketQuote quote(String symbol) {
        throw new IllegalStateException("boom");
    }
}
