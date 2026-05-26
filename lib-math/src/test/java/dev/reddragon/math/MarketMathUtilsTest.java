package dev.reddragon.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Smoke tests for MarketMathUtils — clamp/percent-change/average helpers used
 * by lib-marketdata feature calculators.
 */
class MarketMathUtilsTest {

    private static final double EPS = 1e-9;

    @Test
    void clampClampsToZeroOneRange() {
        assertEquals(0.0, MarketMathUtils.clamp(-1.0), EPS);
        assertEquals(1.0, MarketMathUtils.clamp(2.0), EPS);
        assertEquals(0.5, MarketMathUtils.clamp(0.5), EPS);
    }

    @Test
    void clampRejectsNaN() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MarketMathUtils.clamp(Double.NaN)
        );
    }

    @Test
    void safePercentChangeReturnsZeroWhenPreviousIsZero() {
        assertEquals(0.0, MarketMathUtils.safePercentChange(100.0, 0.0), EPS);
    }

    @Test
    void safePercentChangeReturnsZeroForNonFinitePrevious() {
        assertEquals(0.0, MarketMathUtils.safePercentChange(100.0, Double.NaN), EPS);
        assertEquals(0.0, MarketMathUtils.safePercentChange(100.0, Double.POSITIVE_INFINITY), EPS);
        assertEquals(0.0, MarketMathUtils.safePercentChange(100.0, Double.NEGATIVE_INFINITY), EPS);
    }

    @Test
    void safePercentChangeReturnsZeroForNegativePrevious() {
        // Finance callers never expect a sign flip from a negative denominator;
        // treat it as "no information" rather than silently flipping the result.
        assertEquals(0.0, MarketMathUtils.safePercentChange(110.0, -100.0), EPS);
    }

    @Test
    void safePercentChangeReturnsZeroForNonFiniteCurrent() {
        assertEquals(0.0, MarketMathUtils.safePercentChange(Double.NaN, 100.0), EPS);
        assertEquals(0.0, MarketMathUtils.safePercentChange(Double.POSITIVE_INFINITY, 100.0), EPS);
    }

    @Test
    void safePercentChangeComputesCorrectDelta() {
        assertEquals(0.10, MarketMathUtils.safePercentChange(110.0, 100.0), EPS);
        assertEquals(-0.05, MarketMathUtils.safePercentChange(95.0, 100.0), EPS);
    }

    @Test
    void averageReturnsMidpoint() {
        assertEquals(50.0, MarketMathUtils.average(0.0, 100.0), EPS);
    }

    @Test
    void floorAtZeroPinsNegativesToZero() {
        assertEquals(0.0, MarketMathUtils.floorAtZero(-3.5), EPS);
    }

    @Test
    void floorAtZeroLeavesPositivesUnchanged() {
        assertEquals(2.7, MarketMathUtils.floorAtZero(2.7), EPS);
        assertEquals(0.0, MarketMathUtils.floorAtZero(0.0), EPS);
    }

    @Test
    void floorAtZeroPreservesPositiveInfinity() {
        // Unlike clamp(), floorAtZero has no upper bound.
        assertEquals(Double.POSITIVE_INFINITY, MarketMathUtils.floorAtZero(Double.POSITIVE_INFINITY), EPS);
    }

    @Test
    void floorAtZeroRejectsNaN() {
        assertThrows(IllegalArgumentException.class,
                () -> MarketMathUtils.floorAtZero(Double.NaN));
    }
}
