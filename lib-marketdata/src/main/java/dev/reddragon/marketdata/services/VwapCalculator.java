package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;

import java.util.List;
import java.util.Objects;

/**
 * Calculates volume-weighted average price from intraday bars.
 */
public class VwapCalculator {

    /**
     * Main processing flow.
     */
    public double process(List<IntradayBar> bars) {
        Objects.requireNonNull(bars, "bars are required");

        if (bars.isEmpty()) {
            return 0.0;
        }

        double weightedPriceTotal = weightedPriceTotal(bars);
        long volumeTotal = volumeTotal(bars);

        if (volumeTotal == 0) {
            return 0.0;
        }

        return weightedPriceTotal / volumeTotal;
    }

    private double weightedPriceTotal(List<IntradayBar> bars) {
        double total = 0.0;

        for (IntradayBar bar : bars) {
            total += typicalPrice(bar) * bar.volume();
        }

        return total;
    }

    private long volumeTotal(List<IntradayBar> bars) {
        long total = 0L;

        for (IntradayBar bar : bars) {
            total += bar.volume();
        }

        return total;
    }

    private double typicalPrice(IntradayBar bar) {
        return (bar.high() + bar.low() + bar.close()) / 3.0;
    }
}
