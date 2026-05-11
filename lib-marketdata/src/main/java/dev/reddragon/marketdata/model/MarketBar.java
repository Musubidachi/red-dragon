package dev.reddragon.marketdata.model;

import java.time.LocalDate;

public record MarketBar(
        String symbol,
        LocalDate date,
        double open,
        double high,
        double low,
        double close,
        long volume
) {
    public MarketBar {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        symbol = symbol.trim().toUpperCase();
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
    }

    public double range() {
        return high - low;
    }
}
