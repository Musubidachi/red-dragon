package dev.reddragon.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Smoke tests for ValidationScoreUtils — the strict-validation cousin of
 * AnalyticsScoreUtils that throws instead of clamping.
 */
class ValidationScoreUtilsTest {

    @Test
    void requireNormalizedAcceptsValuesInRange() {
        assertEquals(0.5, ValidationScoreUtils.requireNormalized("score", 0.5));
        assertEquals(0.0, ValidationScoreUtils.requireNormalized("score", 0.0));
        assertEquals(1.0, ValidationScoreUtils.requireNormalized("score", 1.0));
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
    void weightedAverageReturnsZeroForZeroWeight() {
        assertEquals(0.0, ValidationScoreUtils.weightedAverage(10.0, 0.0));
    }

    @Test
    void weightedAverageDividesWhenWeightNonZero() {
        assertEquals(0.5, ValidationScoreUtils.weightedAverage(2.0, 4.0));
    }
}
