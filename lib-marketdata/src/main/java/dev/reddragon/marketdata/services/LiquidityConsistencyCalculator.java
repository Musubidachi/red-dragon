package dev.reddragon.marketdata.services;

import dev.reddragon.marketdata.models.OrderBookSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * Scores consistency of liquidity conditions over time.
 */
public class LiquidityConsistencyCalculator {

    /**
     * Main processing flow.
     */
    public double process(List<OrderBookSnapshot> snapshots) {
        Objects.requireNonNull(snapshots, "snapshots are required");

        if (snapshots.isEmpty()) {
            return 0.0;
        }

        double spreadAverage = spreadAverage(snapshots);
        double imbalanceAverage = imbalanceAverage(snapshots);

        double spreadScore = spreadScore(spreadAverage);
        double imbalanceScore = 1.0 - Math.abs(imbalanceAverage - 0.50);

        return Math.max(0.0, Math.min(1.0,
                spreadScore * 0.60
                        + imbalanceScore * 0.40
        ));
    }

    private double spreadAverage(List<OrderBookSnapshot> snapshots) {
        double total = 0.0;

        for (OrderBookSnapshot snapshot : snapshots) {
            total += snapshot.spreadPercent();
        }

        return total / snapshots.size();
    }

    private double imbalanceAverage(List<OrderBookSnapshot> snapshots) {
        double total = 0.0;

        for (OrderBookSnapshot snapshot : snapshots) {
            total += snapshot.imbalanceScore();
        }

        return total / snapshots.size();
    }

    private double spreadScore(double spreadAverage) {
        if (spreadAverage <= 0.001) {
            return 1.0;
        }

        if (spreadAverage <= 0.003) {
            return 0.80;
        }

        if (spreadAverage <= 0.008) {
            return 0.55;
        }

        return 0.25;
    }
}
