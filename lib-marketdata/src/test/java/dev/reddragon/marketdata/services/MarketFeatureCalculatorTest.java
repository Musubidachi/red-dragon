package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for MarketFeatureCalculator - the lib-marketdata entry point that
 * turns a list of bars into a MarketDataSnapshot.
 */
class MarketFeatureCalculatorTest {

    private final MarketFeatureCalculator calculator = new MarketFeatureCalculator();

    @Test
    void blankSymbolProducesMissingSymbolQuality() {
        MarketDataSnapshot snapshot = calculator.process("   ", bars(20));
        assertEquals(MarketDataQuality.MISSING_SYMBOL, snapshot.quality());
    }

    @Test
    void emptyBarsProducesEmptyBarsQuality() {
        MarketDataSnapshot snapshot = calculator.process("ACME", List.of());
        assertEquals(MarketDataQuality.EMPTY_BARS, snapshot.quality());
    }

    @Test
    void singleBarProducesInsufficientHistoryQuality() {
        MarketDataSnapshot snapshot = calculator.process("ACME", bars(1));
        assertEquals(MarketDataQuality.INSUFFICIENT_HISTORY, snapshot.quality());
    }

    @Test
    void liquidBarsProduceCompleteSnapshot() {
        MarketDataSnapshot snapshot = calculator.process("ACME", bars(20));
        assertEquals(MarketDataQuality.COMPLETE, snapshot.quality());
        assertEquals("ACME", snapshot.symbol());
        assertTrue(snapshot.latestClose() > 0);
        assertTrue(snapshot.averageTrueRange() > 0);
        // Scoring is no longer this layer's responsibility — the snapshot
        // carries placeholder zeros until the lib-analytics scorer enriches
        // it. See lib-marketdata REVIEW.md Finding #8 architectural fix.
        assertEquals(0.0, snapshot.liquidityScore());
        assertEquals(0.0, snapshot.volatilityStabilityScore());
    }

    @Test
    void illiquidBarsProduceIlliquidQuality() {
        MarketDataSnapshot snapshot = calculator.process("ACME", barsWithVolume(20, 5_000));
        assertEquals(MarketDataQuality.ILLIQUID, snapshot.quality());
    }

    @Test
    void rangePositionIsAlwaysWithinZeroOne() {
        MarketDataSnapshot snapshot = calculator.process("ACME", bars(20));
        assertTrue(snapshot.rangePosition() >= 0.0 && snapshot.rangePosition() <= 1.0);
    }

    @Test
    void unsortedBarsStillProduceCoherentSnapshot() {
        // Pass bars in reverse order; the calculator should sort them.
        List<MarketBar> reversed = new ArrayList<>(bars(20));
        java.util.Collections.reverse(reversed);
        MarketDataSnapshot snapshot = calculator.process("ACME", reversed);
        assertEquals(MarketDataQuality.COMPLETE, snapshot.quality());
    }

    private List<MarketBar> bars(int count) {
        return barsWithVolume(count, 1_000_000);
    }

    private List<MarketBar> barsWithVolume(int count, long volume) {
        LocalDate start = LocalDate.parse("2026-04-01");
        List<MarketBar> bars = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double base = 100.0 + i * 0.5;
            bars.add(new MarketBar("ACME", start.plusDays(i),
                    base, base + 1.0, base - 1.0, base + 0.3, volume));
        }
        return bars;
    }
}
