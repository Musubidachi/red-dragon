package dev.reddragon.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Smoke tests for AnalyticsScoreUtils — the shared clamp/average/weighted-average
 * helpers used by every L3-L8 scorer in lib-analytics.
 */
class AnalyticsScoreUtilsTest {

    private static final double EPS = 1e-9;

    @Test
    void clampPinsBelowZeroToZero() {
        assertEquals(0.0, AnalyticsScoreUtils.clamp(-0.5), EPS);
    }

    @Test
    void clampPinsAboveOneToOne() {
        assertEquals(1.0, AnalyticsScoreUtils.clamp(1.7), EPS);
    }

    @Test
    void clampLeavesNormalizedValuesAlone() {
        assertEquals(0.42, AnalyticsScoreUtils.clamp(0.42), EPS);
        assertEquals(0.0, AnalyticsScoreUtils.clamp(0.0), EPS);
        assertEquals(1.0, AnalyticsScoreUtils.clamp(1.0), EPS);
    }

    @Test
    void clampRejectsNaN() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AnalyticsScoreUtils.clamp(Double.NaN)
        );
        assertEquals("clamp: value must not be NaN", ex.getMessage());
    }

    @Test
    void clampHandlesInfinityAtTheBound() {
        // Positive infinity is greater than 1.0, so it clamps to 1.0.
        assertEquals(1.0, AnalyticsScoreUtils.clamp(Double.POSITIVE_INFINITY), EPS);
        // Negative infinity is less than 0.0, so it clamps to 0.0.
        assertEquals(0.0, AnalyticsScoreUtils.clamp(Double.NEGATIVE_INFINITY), EPS);
    }

    @Test
    void averageReturnsMidpoint() {
        assertEquals(0.5, AnalyticsScoreUtils.average(0.0, 1.0), EPS);
        assertEquals(0.3, AnalyticsScoreUtils.average(0.2, 0.4), EPS);
    }

    @Test
    void weightedAverageReturnsZeroForZeroWeight() {
        assertEquals(0.0, AnalyticsScoreUtils.weightedAverage(1.0, 0.0), EPS);
    }

    @Test
    void weightedAverageDividesWhenWeightNonZero() {
        assertEquals(0.5, AnalyticsScoreUtils.weightedAverage(2.0, 4.0), EPS);
    }
}
