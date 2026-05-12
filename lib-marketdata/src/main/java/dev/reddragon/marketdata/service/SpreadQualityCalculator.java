package dev.reddragon.marketdata.service;

import dev.reddragon.marketdata.model.OrderBookSnapshot;
import dev.reddragon.marketdata.util.MarketMathUtils;

import java.util.Objects;

/**
 * Scores spread quality from normalized order-book data.
 */
public class SpreadQualityCalculator {

    /**
     * Main processing flow.
     */
    public double process(OrderBookSnapshot orderBook) {
        Objects.requireNonNull(orderBook, "orderBook is required");

        double spread = orderBook.spreadPercent();

        if (spread <= 0.001) {
            return 1.0;
        }

        if (spread <= 0.003) {
            return 0.85;
        }

        if (spread <= 0.007) {
            return 0.65;
        }

        if (spread <= 0.015) {
            return 0.40;
        }

        return 0.15;
    }
}
