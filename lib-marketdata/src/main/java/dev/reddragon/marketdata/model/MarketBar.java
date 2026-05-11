package dev.reddragon.marketdata.model;

import lombok.Value;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Value
@Accessors(fluent = true)
public class MarketBar {
    String symbol;
    LocalDate date;
    double open;
    double high;
    double low;
    double close;
    long volume;

    public MarketBar(
            String symbol,
            LocalDate date,
            double open,
            double high,
            double low,
            double close,
            long volume
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (high < low) {
            throw new IllegalArgumentException("high cannot be below low");
        }
        if (open < 0 || high < 0 || low < 0 || close < 0) {
            throw new IllegalArgumentException("prices must be non-negative");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("volume must be non-negative");
        }

        this.symbol = symbol.trim().toUpperCase();
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public double range() {
        return high - low;
    }
}
