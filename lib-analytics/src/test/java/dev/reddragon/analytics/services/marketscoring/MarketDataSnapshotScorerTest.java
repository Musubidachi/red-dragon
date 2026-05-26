package dev.reddragon.analytics.services.marketscoring;

import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the architectural Finding #8 fix in lib-marketdata —
 * scoring policy now lives here, and an unconfigured deployment must
 * exactly reproduce the historical bucket values that {@code
 * MarketFeatureCalculator} used to compute inline.
 */
class MarketDataSnapshotScorerTest {

    private final MarketDataSnapshotScorer scorer = new MarketDataSnapshotScorer();

    @Test
    void nullSnapshotPassesThrough() {
        assertNull(scorer.process(null));
    }

    @Test
    void highVolumeSnapshotsScoreNearOne() {
        // 6M average daily volume → above the LIQUIDITY_VOLUME_HIGH (5M)
        // bucket → score = 1.00.
        MarketDataSnapshot raw = snapshot(100.0, 2.0, 6_000_000);
        MarketDataSnapshot scored = scorer.process(raw);
        assertEquals(1.00, scored.liquidityScore(), 1e-9);
    }

    @Test
    void thinVolumeSnapshotsScoreLowest() {
        // 50 average volume → below the LIQUIDITY_VOLUME_LOW (100k) bucket
        // → score = 0.15 (thin).
        MarketDataSnapshot raw = snapshot(100.0, 2.0, 50);
        MarketDataSnapshot scored = scorer.process(raw);
        assertEquals(0.15, scored.liquidityScore(), 1e-9);
    }

    @Test
    void lowAtrPercentScoresStability() {
        // ATR / close = 1 / 100 = 0.01 → below VERY_LOW (0.03) bucket →
        // stability = 0.90.
        MarketDataSnapshot raw = snapshot(100.0, 1.0, 1_000_000);
        MarketDataSnapshot scored = scorer.process(raw);
        assertEquals(0.90, scored.volatilityStabilityScore(), 1e-9);
    }

    @Test
    void highAtrPercentScoresLeastStable() {
        // ATR / close = 30 / 100 = 0.30 → above HIGH (0.15) bucket →
        // stability = 0.15.
        MarketDataSnapshot raw = snapshot(100.0, 30.0, 1_000_000);
        MarketDataSnapshot scored = scorer.process(raw);
        assertEquals(0.15, scored.volatilityStabilityScore(), 1e-9);
    }

    @Test
    void undefinedAtrPercentReturnsUnknownSentinel() {
        // Zero close means ATR% is undefined → 0.50 sentinel.
        MarketDataSnapshot raw = snapshot(0.0, 0.0, 1_000_000);
        MarketDataSnapshot scored = scorer.process(raw);
        assertEquals(0.50, scored.volatilityStabilityScore(), 1e-9);
    }

    @Test
    void scorerOnlyTouchesScoreFields() {
        // The other 13 fields must come through unchanged.
        MarketDataSnapshot raw = snapshot(100.0, 2.0, 6_000_000);
        MarketDataSnapshot scored = scorer.process(raw);
        assertEquals(raw.symbol(),                scored.symbol());
        assertEquals(raw.observedAt(),            scored.observedAt());
        assertEquals(raw.latestClose(),           scored.latestClose(), 1e-12);
        assertEquals(raw.previousClose(),         scored.previousClose(), 1e-12);
        assertEquals(raw.averageTrueRange(),      scored.averageTrueRange(), 1e-12);
        assertEquals(raw.averageVolume(),         scored.averageVolume(), 1e-12);
        assertEquals(raw.quality(),               scored.quality());
        assertTrue(scored.liquidityScore() > 0.0, "scorer must have written a real score");
    }

    private MarketDataSnapshot snapshot(double latestClose, double atr, double averageVolume) {
        return new MarketDataSnapshot(
                "ACME",
                Instant.parse("2026-05-13T21:00:00Z"),
                latestClose,
                latestClose,
                0.0,
                atr,
                0.5,
                averageVolume,
                /* liquidityScore */ 0.0,
                /* volatilityStabilityScore */ 0.0,
                1.0,
                0.0,
                0.5,
                MarketDataQuality.COMPLETE,
                List.of()
        );
    }
}
