package dev.reddragon.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Smoke tests for AnalyticsScoreUtils — the shared clamp/average/weighted-average
 * helpers used by every L3-L8 scorer in lib-analytics.
 */
class AnalyticsScoreUtilsTest {

    @Test
    void clampPinsBelowZeroToZero() {
        assertEquals(0.0, AnalyticsScoreUtils.clamp(-0.5));
    }

    @Test
    void clampPinsAboveOneToOne() {
        assertEquals(1.0, AnalyticsScoreUtils.clamp(1.7));
    }

    @Test
    void clampLeavesNormalizedValuesAlone() {
        assertEquals(0.42, AnalyticsScoreUtils.clamp(0.42));
        assertEquals(0.0, AnalyticsScoreUtils.clamp(0.0));
        assertEquals(1.0, AnalyticsScoreUtils.clamp(1.0));
    }

    @Test
    void averageReturnsMidpoint() {
        assertEquals(0.5, AnalyticsScoreUtils.average(0.0, 1.0));
        assertEquals(0.3, AnalyticsScoreUtils.average(0.2, 0.4), 1e-9);
    }

    @Test
    void weightedAverageReturnsZeroForZeroWeight() {
        assertEquals(0.0, AnalyticsScoreUtils.weightedAverage(1.0, 0.0));
    }

    @Test
    void weightedAverageDividesWhenWeightNonZero() {
        assertEquals(0.5, AnalyticsScoreUtils.weightedAverage(2.0, 4.0));
    }
}
