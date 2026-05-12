package dev.reddragon.marketdata.model;

import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;

@Value
@Accessors(fluent = true)
public class IntradayBar {
    String symbol;
    Instant startTime;
    double open;
    double high;
    double low;
    double close;
    long volume;
    double vwap;

    public IntradayBar(
            String symbol,
            Instant startTime,
            double open,
            double high,
            double low,
            double close,
            long volume,
            double vwap
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("startTime is required");
        }
        if (high < low) {
            throw new IllegalArgumentException("high cannot be below low");
        }
        if (open < 0 || high < 0 || low < 0 || close < 0 || vwap < 0) {
            throw new IllegalArgumentException("prices must be non-negative");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("volume must be non-negative");
        }

        this.symbol = symbol.trim().toUpperCase();
        this.startTime = startTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.vwap = vwap;
    }

    public double range() {
        return high - low;
    }
}
