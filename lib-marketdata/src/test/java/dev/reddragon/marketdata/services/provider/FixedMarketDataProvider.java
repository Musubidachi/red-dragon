package dev.reddragon.marketdata.services.provider;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;

class FixedMarketDataProvider implements MarketDataProvider {
    private final List<MarketBar> dailyBars;
    private final List<IntradayBar> intradayBars;
    private final MarketQuote quote;

    FixedMarketDataProvider(List<MarketBar> dailyBars, List<IntradayBar> intradayBars, MarketQuote quote) {
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
