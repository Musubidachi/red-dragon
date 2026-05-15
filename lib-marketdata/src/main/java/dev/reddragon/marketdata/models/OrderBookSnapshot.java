package dev.reddragon.marketdata.models;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class OrderBookSnapshot {
    String symbol;
    double bid;
    double ask;
    long bidSize;
    long askSize;
    double spreadPercent;
    double imbalanceScore;

    public OrderBookSnapshot(
            String symbol,
            double bid,
            double ask,
            long bidSize,
            long askSize,
            double spreadPercent,
            double imbalanceScore
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }

        this.symbol = symbol.trim().toUpperCase();
        this.bid = bid;
        this.ask = ask;
        this.bidSize = bidSize;
        this.askSize = askSize;
        this.spreadPercent = spreadPercent;
        this.imbalanceScore = imbalanceScore;
    }
}
