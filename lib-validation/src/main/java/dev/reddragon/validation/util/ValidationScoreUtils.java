package dev.reddragon.validation.util;

import lombok.experimental.UtilityClass;

/**
 * Shared validation scoring helpers.
 */
@UtilityClass
public class ValidationScoreUtils {

    public double requireNormalized(String fieldName, double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }

    public void requireNonNegative(String fieldName, double value) {
        if (value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }

    public double weightedAverage(double weightedTotal, double totalWeight) {
        return totalWeight == 0.0 ? 0.0 : weightedTotal / totalWeight;
    }
}
