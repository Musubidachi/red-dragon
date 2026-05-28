package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketContextSnapshotTest {

    @Test
    void marketLiquidityTextureNormalizesSymbolAndClampsScores() {
        MarketLiquidityTextureSnapshot snapshot = new MarketLiquidityTextureSnapshot(
                " abc ", -0.2, 0.25, 0.5, 0.75, 1.4);

        assertEquals("ABC", snapshot.symbol());
        assertEquals(0.0, snapshot.spreadQualityScore());
        assertEquals(0.25, snapshot.orderBookDepthScore());
        assertEquals(0.5, snapshot.liquidityConsistencyScore());
        assertEquals(0.75, snapshot.slippageRiskScore());
        assertEquals(1.0, snapshot.relativeVolumeScore());
    }

    @Test
    void marketIntradayStructurePreservesProviderContextAndClampsScores() {
        MarketIntradayStructureSnapshot snapshot = new MarketIntradayStructureSnapshot(
                " xyz ", 14.25, 14.70, 3.2, 1.3, -0.4, 0.6, 2.0, true);

        assertEquals("XYZ", snapshot.symbol());
        assertEquals(14.25, snapshot.sessionVwap());
        assertEquals(14.70, snapshot.latestClose());
        assertEquals(3.2, snapshot.vwapDistancePercent());
        assertEquals(1.0, snapshot.vwapReclaimStrength());
        assertEquals(0.0, snapshot.directionalPersistenceScore());
        assertEquals(0.6, snapshot.rotationalQualityScore());
        assertEquals(1.0, snapshot.intradayTrendStrength());
        assertTrue(snapshot.aboveVwap());
    }

    @Test
    void marketVolatilityExpansionPreservesProviderContextAndClampsScores() {
        MarketVolatilityExpansionSnapshot snapshot = new MarketVolatilityExpansionSnapshot(
                " def ", 1.6, 0.9, 1.8, -0.3, 1.2);

        assertEquals("DEF", snapshot.symbol());
        assertEquals(1.6, snapshot.currentAtr());
        assertEquals(0.9, snapshot.baselineAtr());
        assertEquals(1.8, snapshot.realizedVolatility());
        assertEquals(0.0, snapshot.volatilityExpansionScore());
        assertEquals(1.0, snapshot.volatilityCompressionScore());
    }

    @Test
    void marketContextSnapshotsRejectBlankSymbols() {
        assertThrows(IllegalArgumentException.class, () -> new MarketLiquidityTextureSnapshot(
                " ", 0.1, 0.2, 0.3, 0.4, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new MarketIntradayStructureSnapshot(
                " ", 1.0, 1.1, 0.1, 0.2, 0.3, 0.4, 0.5, false));
        assertThrows(IllegalArgumentException.class, () -> new MarketVolatilityExpansionSnapshot(
                " ", 1.0, 0.8, 1.1, 0.4, 0.5));
    }

    @Test
    void marketContextSnapshotsRejectNanScores() {
        assertThrows(IllegalArgumentException.class, () -> new MarketLiquidityTextureSnapshot(
                "ABC", Double.NaN, 0.2, 0.3, 0.4, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new MarketIntradayStructureSnapshot(
                "ABC", 1.0, 1.1, 0.1, Double.NaN, 0.3, 0.4, 0.5, false));
        assertThrows(IllegalArgumentException.class, () -> new MarketVolatilityExpansionSnapshot(
                "ABC", 1.0, 0.8, 1.1, Double.NaN, 0.5));
    }

    @Test
    void marketStateSignalDefaultsRegimeClampsScoresAndCopiesNotes() {
        List<String> sourceNotes = new ArrayList<>(List.of("breadth improving"));

        MarketStateSignal signal = new MarketStateSignal(
                null, 1.3, -0.2, true, sourceNotes);
        sourceNotes.add("late mutation");

        assertEquals(RegimeLabel.MIXED, signal.regimeLabel());
        assertEquals(1.0, signal.confidence());
        assertEquals(0.0, signal.equilibriumRestorationProbability());
        assertTrue(signal.deploymentSupported());
        assertEquals(List.of("breadth improving"), signal.notes());
        assertThrows(UnsupportedOperationException.class, () -> signal.notes().add("mutation"));
    }
}
