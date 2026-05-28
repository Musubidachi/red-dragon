package dev.reddragon.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoreMathUtilsTest {

    private static final double EPS = 1e-9;

    @Test
    void clampPinsValuesToZeroOneRange() {
        assertEquals(0.0, CoreMathUtils.clamp(-0.25), EPS);
        assertEquals(0.25, CoreMathUtils.clamp(0.25), EPS);
        assertEquals(1.0, CoreMathUtils.clamp(1.25), EPS);
    }

    @Test
    void clampRejectsNaN() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CoreMathUtils.clamp(Double.NaN)
        );
        assertEquals("clamp: value must not be NaN", ex.getMessage());
    }

    @Test
    void clampPinsInfinitiesAtTheBounds() {
        assertEquals(0.0, CoreMathUtils.clamp(Double.NEGATIVE_INFINITY), EPS);
        assertEquals(1.0, CoreMathUtils.clamp(Double.POSITIVE_INFINITY), EPS);
    }

    @Test
    void averageReturnsMidpoint() {
        assertEquals(0.5, CoreMathUtils.average(0.0, 1.0), EPS);
        assertEquals(4.0, CoreMathUtils.average(2.0, 6.0), EPS);
    }

    @Test
    void weightedAverageReturnsNoSignalSentinelForZeroWeight() {
        assertEquals(0.0, CoreMathUtils.weightedAverage(10.0, 0.0), EPS);
    }

    @Test
    void weightedAverageDividesWhenWeightIsNonZero() {
        assertEquals(0.75, CoreMathUtils.weightedAverage(3.0, 4.0), EPS);
    }
}
