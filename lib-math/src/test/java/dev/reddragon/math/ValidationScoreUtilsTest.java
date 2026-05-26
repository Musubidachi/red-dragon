package dev.reddragon.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for ValidationScoreUtils — the strict-validation cousin of
 * AnalyticsScoreUtils that throws instead of clamping.
 */
class ValidationScoreUtilsTest {

    private static final double EPS = 1e-9;

    @Test
    void requireNormalizedAcceptsValuesInRange() {
        assertEquals(0.5, ValidationScoreUtils.requireNormalized("score", 0.5), EPS);
        assertEquals(0.0, ValidationScoreUtils.requireNormalized("score", 0.0), EPS);
        assertEquals(1.0, ValidationScoreUtils.requireNormalized("score", 1.0), EPS);
    }

    @Test
    void requireNormalizedRejectsBelowZero() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ValidationScoreUtils.requireNormalized("score", -0.01)
        );
        assertTrue(ex.getMessage().contains("score"), "message should name the field");
    }

    @Test
    void requireNormalizedRejectsAboveOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ValidationScoreUtils.requireNormalized("score", 1.01)
        );
    }

    @Test
    void requireNormalizedRejectsNaN() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ValidationScoreUtils.requireNormalized("score", Double.NaN)
        );
        assertTrue(ex.getMessage().contains("score"), "message should name the field");
        assertTrue(ex.getMessage().contains("NaN"), "message should mention NaN");
    }

    @Test
    void requireNonNegativeAcceptsZeroAndPositives() {
        assertDoesNotThrow(() -> ValidationScoreUtils.requireNonNegative("weight", 0.0));
        assertDoesNotThrow(() -> ValidationScoreUtils.requireNonNegative("weight", 5.0));
    }

    @Test
    void requireNonNegativeRejectsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ValidationScoreUtils.requireNonNegative("weight", -1.0)
        );
    }

    @Test
    void requireNonNegativeRejectsNaN() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ValidationScoreUtils.requireNonNegative("weight", Double.NaN)
        );
        assertTrue(ex.getMessage().contains("weight"), "message should name the field");
    }

    @Test
    void requireNonNegativeRejectsPositiveInfinity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ValidationScoreUtils.requireNonNegative("weight", Double.POSITIVE_INFINITY)
        );
    }

    @Test
    void weightedAverageReturnsZeroForZeroWeight() {
        assertEquals(0.0, ValidationScoreUtils.weightedAverage(10.0, 0.0), EPS);
    }

    @Test
    void weightedAverageDividesWhenWeightNonZero() {
        assertEquals(0.5, ValidationScoreUtils.weightedAverage(2.0, 4.0), EPS);
    }
}
