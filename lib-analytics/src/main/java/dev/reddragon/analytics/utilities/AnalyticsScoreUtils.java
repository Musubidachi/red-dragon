package dev.reddragon.analytics.utilities;

import lombok.experimental.UtilityClass;

/**
 * Shared scoring helpers for deterministic analytics calculations.
 */
@UtilityClass
public class AnalyticsScoreUtils {

    public double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    public double average(double left, double right) {
        return (left + right) / 2.0;
    }

    public double weightedAverage(double weightedTotal, double totalWeight) {
        return totalWeight == 0.0 ? 0.0 : weightedTotal / totalWeight;
    }
}
