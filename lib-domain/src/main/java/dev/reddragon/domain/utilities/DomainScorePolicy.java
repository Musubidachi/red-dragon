package dev.reddragon.domain.utilities;

import dev.reddragon.math.AnalyticsScoreUtils;
import dev.reddragon.math.ValidationScoreUtils;

/**
 * Documents and centralizes score handling for shared domain value objects.
 *
 * <p>Rule of thumb:
 * <ul>
 *   <li>External candidate inputs and validation outputs reject scores outside
 *       0.0-1.0.</li>
 *   <li>Derived scorer, market-context, adversarial, and exit snapshots clamp
 *       scores to 0.0-1.0 at module boundaries.</li>
 * </ul>
 */
public final class DomainScorePolicy {

    private DomainScorePolicy() {
        throw new AssertionError("utility class");
    }

    public static double requireInputScore(String fieldName, double value) {
        return ValidationScoreUtils.requireNormalized(fieldName, value);
    }

    public static double clampDerivedScore(double value) {
        return AnalyticsScoreUtils.clamp(value);
    }
}
