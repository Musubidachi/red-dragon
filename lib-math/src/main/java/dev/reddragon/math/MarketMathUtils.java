package dev.reddragon.math;

import lombok.experimental.UtilityClass;

/**
 * Shared numeric helpers for market-data normalization and feature calculation.
 */
@UtilityClass
public class MarketMathUtils {

    /**
     * Clamp a value into the normalized {@code [0.0, 1.0]} range.
     *
     * <p>{@code NaN} is treated as a programmer error and rejected; see
     * {@link AnalyticsScoreUtils#clamp(double)} for the rationale.
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

    /**
     * Fractional percent change of {@code current} relative to {@code previous}.
     *
     * <p>Returns {@code 0.0} as the "no information" sentinel whenever the
     * denominator is unusable: zero, negative, {@code NaN}, or infinite. A
     * negative denominator is excluded because finance callers (price ratios)
     * never expect a sign flip from {@code (current - previous) / previous}
     * when {@code previous &lt; 0}; if a non-finance caller needs that semantic
     * they should not use this helper.
     *
     * <p>{@code current} that is {@code NaN} or infinite also yields {@code 0.0}
     * to preserve the "no information" contract.
     */
    public double safePercentChange(double current, double previous) {
        if (!Double.isFinite(previous) || previous <= 0.0) {
            return 0.0;
        }
        if (!Double.isFinite(current)) {
            return 0.0;
        }
        return (current - previous) / previous;
    }

    public double average(double left, double right) {
        return (left + right) / 2.0;
    }

    /**
     * Lower-bound a value at zero. Unlike {@link #clamp(double)}, this imposes
     * no upper bound — useful for ratios that can legitimately exceed 1.0
     * (e.g. relative volume on a heavy day).
     *
     * <p>{@code NaN} is rejected as a programmer error rather than silently
     * passing through (the same rationale as {@link #clamp(double)}). Positive
     * infinity is preserved.
     *
     * @throws IllegalArgumentException if {@code value} is {@code NaN}
     */
    public double floorAtZero(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("floorAtZero: value must not be NaN");
        }
        return Math.max(0.0, value);
    }
}
