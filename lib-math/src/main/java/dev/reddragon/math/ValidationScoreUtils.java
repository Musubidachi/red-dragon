package dev.reddragon.math;

import lombok.experimental.UtilityClass;

/**
 * Shared validation scoring helpers.
 */
@UtilityClass
public class ValidationScoreUtils {

    /**
     * Strict guardrail asserting {@code value} is in the normalized {@code [0.0, 1.0]}
     * range.
     *
     * <p>Unlike {@link AnalyticsScoreUtils#clamp(double)} this throws rather
     * than coercing. {@code NaN} is rejected explicitly because {@code NaN < 0.0}
     * and {@code NaN > 1.0} both evaluate to {@code false} in IEEE-754, which
     * would otherwise let {@code NaN} fall through and poison downstream
     * weighted aggregations.
     *
     * @throws IllegalArgumentException if {@code value} is {@code NaN} or outside
     *         {@code [0.0, 1.0]}
     */
    public double requireNormalized(String fieldName, double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException(fieldName + " must not be NaN");
        }
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }

    /**
     * Strict guardrail asserting {@code value} is a finite, non-negative weight.
     *
     * <p>Rejects {@code NaN} and {@code +Infinity} explicitly: a weight of
     * {@code +Infinity} in {@link #weightedAverage} produces an infinite numerator
     * and denominator, returning {@code NaN}, which poisons the validation
     * engine's score.
     *
     * @throws IllegalArgumentException if {@code value} is {@code NaN}, infinite,
     *         or negative
     */
    public void requireNonNegative(String fieldName, double value) {
        if (!(value >= 0.0 && Double.isFinite(value))) {
            throw new IllegalArgumentException(
                    fieldName + " must be a finite non-negative number"
            );
        }
    }

    public double weightedAverage(double weightedTotal, double totalWeight) {
        return totalWeight == 0.0 ? 0.0 : weightedTotal / totalWeight;
    }
}
