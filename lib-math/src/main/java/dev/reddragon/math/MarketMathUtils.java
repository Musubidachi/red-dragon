package dev.reddragon.math;

import lombok.experimental.UtilityClass;

/**
 * Shared numeric helpers for market-data normalization and feature calculation.
 */
@UtilityClass
public class MarketMathUtils {

    public double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    public double safePercentChange(double current, double previous) {
        return previous == 0.0 ? 0.0 : (current - previous) / previous;
    }

    public double average(double left, double right) {
        return (left + right) / 2.0;
    }
}
