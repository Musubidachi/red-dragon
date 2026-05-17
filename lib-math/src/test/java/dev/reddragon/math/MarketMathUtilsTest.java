package dev.reddragon.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Smoke tests for MarketMathUtils — clamp/percent-change/average helpers used
 * by lib-marketdata feature calculators.
 */
class MarketMathUtilsTest {

    @Test
    void clampClampsToZeroOneRange() {
        assertEquals(0.0, MarketMathUtils.clamp(-1.0));
        assertEquals(1.0, MarketMathUtils.clamp(2.0));
        assertEquals(0.5, MarketMathUtils.clamp(0.5));
    }

    @Test
    void safePercentChangeReturnsZeroWhenPreviousIsZero() {
        assertEquals(0.0, MarketMathUtils.safePercentChange(100.0, 0.0));
    }

    @Test
    void safePercentChangeComputesCorrectDelta() {
        assertEquals(0.10, MarketMathUtils.safePercentChange(110.0, 100.0), 1e-9);
        assertEquals(-0.05, MarketMathUtils.safePercentChange(95.0, 100.0), 1e-9);
    }

    @Test
    void averageReturnsMidpoint() {
        assertEquals(50.0, MarketMathUtils.average(0.0, 100.0));
    }
}
