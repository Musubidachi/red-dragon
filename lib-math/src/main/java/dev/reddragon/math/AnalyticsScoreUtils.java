package dev.reddragon.math;

import lombok.experimental.UtilityClass;

/**
 * Shared scoring helpers for deterministic analytics calculations.
 */
@UtilityClass
public class AnalyticsScoreUtils {

    /**
     * Clamp a value into the normalized {@code [0.0, 1.0]} range.
     *
     * <p>This is a guardrail for value-object constructors (per ARCHITECTURE.md:
     * "If a field has a normalized range, the constructor must enforce it — there
     * is no second line of defense"). {@code NaN} is treated as an explicit
     * programmer error rather than silently passing through, because a {@code NaN}
     * field poisons every downstream weighted aggregation.
     *
     * @throws IllegalArgumentException if {@code value} is {@code NaN}
     */
    public double clamp(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("clamp: value must not be NaN");
        }
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
