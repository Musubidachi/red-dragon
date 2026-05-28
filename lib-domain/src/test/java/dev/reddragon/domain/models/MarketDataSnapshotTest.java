package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for MarketDataSnapshot - the L2 (Data Ingestion) output shape.
 */
class MarketDataSnapshotTest {

    @Test
    void blankSymbolRejected() {
        assertThrows(IllegalArgumentException.class, () -> snapshot("   ", 1_000_000, 0.8, 0.7));
    }

    @Test
    void clampingPinsAllScoresToZeroOneRange() {
        MarketDataSnapshot s = snapshot("abc", 1_000_000, 2.0, -0.3);
        assertEquals(1.0, s.liquidityScore());
        assertEquals(0.0, s.volatilityStabilityScore());
    }

    @Test
    void symbolIsNormalizedToUpperCase() {
        MarketDataSnapshot s = snapshot(" abc ", 1_000_000, 0.5, 0.5);
        assertEquals("ABC", s.symbol());
    }

    @Test
    void completeIsTrueOnlyForCompleteQuality() {
        MarketDataSnapshot complete = snapshot("ABC", 1_000_000, 0.5, 0.5, MarketDataQuality.COMPLETE);
        MarketDataSnapshot stale = snapshot("ABC", 1_000_000, 0.5, 0.5, MarketDataQuality.STALE);
        assertTrue(complete.complete());
        assertFalse(stale.complete());
    }

    @Test
    void isHostileTriggersOnStaleIlliquidOrLowVolatility() {
        assertTrue(snapshot("X", 1_000_000, 0.8, 0.8, MarketDataQuality.STALE).isHostile());
        assertTrue(snapshot("X", 1_000_000, 0.8, 0.8, MarketDataQuality.ILLIQUID).isHostile());
        assertTrue(snapshot("X", 1_000_000, 0.8, 0.20, MarketDataQuality.COMPLETE).isHostile(),
                "vol stability < 0.25 is hostile even with COMPLETE quality");
        assertFalse(snapshot("X", 1_000_000, 0.8, 0.5, MarketDataQuality.COMPLETE).isHostile());
    }

    @Test
    void liquidityTierBucketsByAverageVolume() {
        assertEquals(LiquidityTier.HIGH,      snapshot("X", 1_500_000, 0.5, 0.5).liquidityTier());
        assertEquals(LiquidityTier.MODERATE,  snapshot("X",   500_000, 0.5, 0.5).liquidityTier());
        assertEquals(LiquidityTier.ADEQUATE,  snapshot("X",   100_000, 0.5, 0.5).liquidityTier());
        assertEquals(LiquidityTier.THIN,      snapshot("X",    10_000, 0.5, 0.5).liquidityTier());
    }

    @Test
    void observedAtIsRequired() {
        assertThrows(NullPointerException.class, () -> new MarketDataSnapshot(
                "ABC",
                null,
                10.0, 9.8, 0.02, 0.20, 0.5,
                1_000_000, 0.5, 0.5, 1.2, 0.0, 0.5,
                MarketDataQuality.COMPLETE, List.of()
        ));
    }

    private MarketDataSnapshot snapshot(String symbol, double avgVol, double liquidity, double vol) {
        return snapshot(symbol, avgVol, liquidity, vol, MarketDataQuality.COMPLETE);
    }

    private MarketDataSnapshot snapshot(String symbol, double avgVol, double liquidity, double vol, MarketDataQuality q) {
        return new MarketDataSnapshot(
                symbol,
                Instant.parse("2026-05-13T00:00:00Z"),
                10.0, 9.8, 0.02, 0.20, 0.5,
                avgVol, liquidity, vol, 1.2, 0.0, 0.5,
                q, List.of()
        );
    }
}
