package dev.reddragon.marketdata.services;

import dev.reddragon.marketdata.models.IntradayBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calculates realized volatility from intraday close-to-close returns.
 */
public class RealizedVolatilityCalculator {

    /**
     * Main processing flow.
     */
    public double process(List<IntradayBar> bars) {
        Objects.requireNonNull(bars, "bars are required");

        if (bars.size() < 2) {
            return 0.0;
        }

        List<Double> returns = returns(bars);
        double averageReturn = average(returns);

        return standardDeviation(returns, averageReturn);
    }

    private List<Double> returns(List<IntradayBar> bars) {
        List<Double> returns = new ArrayList<>();

        for (int index = 1; index < bars.size(); index++) {
            double previousClose = bars.get(index - 1).close();
            double currentClose = bars.get(index).close();

            if (previousClose > 0.0) {
                returns.add((currentClose - previousClose) / previousClose);
            }
        }

        return returns;
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;

        for (Double value : values) {
            total += value;
        }

        return total / values.size();
    }

    private double standardDeviation(
            List<Double> values,
            double average
    ) {
        if (values.isEmpty()) {
            return 0.0;
        }

        double varianceTotal = 0.0;

        for (Double value : values) {
            double difference = value - average;
            varianceTotal += difference * difference;
        }

        return Math.sqrt(varianceTotal / values.size());
    }
}
