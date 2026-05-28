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
        return CoreMathUtils.clamp(value);
    }

    public double average(double left, double right) {
        return CoreMathUtils.average(left, right);
    }

    /**
     * Weighted average with {@code 0.0} as the zero-weight "no signal" sentinel.
     *
     * <p>Callers that need to distinguish "no weighted inputs" from a real zero
     * score must inspect {@code totalWeight} before calling this helper.
     */
    public double weightedAverage(double weightedTotal, double totalWeight) {
        return CoreMathUtils.weightedAverage(weightedTotal, totalWeight);
    }
}
