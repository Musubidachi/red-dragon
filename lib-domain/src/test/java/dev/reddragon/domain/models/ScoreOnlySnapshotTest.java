package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreOnlySnapshotTest {

    @Test
    void liquidityTextureSnapshotClampsDerivedScores() {
        LiquidityTextureSnapshot snapshot = new LiquidityTextureSnapshot(
                -0.2, 0.25, 0.5, 0.75, 1.4);

        assertEquals(0.0, snapshot.spreadQualityScore());
        assertEquals(0.25, snapshot.orderBookDepthScore());
        assertEquals(0.5, snapshot.liquidityConsistencyScore());
        assertEquals(0.75, snapshot.slippageRiskScore());
        assertEquals(1.0, snapshot.relativeVolumeScore());
    }

    @Test
    void intradayStructureSnapshotPreservesContextFreeFieldsAndClampsScores() {
        IntradayStructureSnapshot snapshot = new IntradayStructureSnapshot(
                -2.5, 1.3, -0.4, 0.6, 2.0, true);

        assertEquals(-2.5, snapshot.vwapDistancePercent());
        assertEquals(1.0, snapshot.vwapReclaimStrength());
        assertEquals(0.0, snapshot.directionalPersistenceScore());
        assertEquals(0.6, snapshot.rotationalQualityScore());
        assertEquals(1.0, snapshot.intradayTrendStrength());
        assertTrue(snapshot.aboveVwap());
    }

    @Test
    void volatilityExpansionSnapshotPreservesRawAtrFieldsAndClampsScores() {
        VolatilityExpansionSnapshot snapshot = new VolatilityExpansionSnapshot(
                8.5, 3.5, -0.1, 1.2, 0.4);

        assertEquals(8.5, snapshot.currentAtrPercent());
        assertEquals(3.5, snapshot.baselineAtrPercent());
        assertEquals(0.0, snapshot.impliedVolatilityRankScore());
        assertEquals(1.0, snapshot.realizedVolatilityExpansionScore());
        assertEquals(0.4, snapshot.volatilityCompressionScore());
    }

    @Test
    void fundamentalOptionsAndPropagationSnapshotsClampDerivedScores() {
        FundamentalImpactSnapshot fundamental = new FundamentalImpactSnapshot(
                -0.5, 0.2, 1.5, 0.6, 0.8);
        OptionsFlowSnapshot options = new OptionsFlowSnapshot(
                1.2, -0.3, 0.4, 0.7, 2.0);
        PropagationSnapshot propagation = new PropagationSnapshot(
                -1.0, 0.3, 0.5, 1.1, 0.9);

        assertEquals(0.0, fundamental.revenueImpactScore());
        assertEquals(0.2, fundamental.marketCapRelativeImpactScore());
        assertEquals(1.0, fundamental.structuralDemandShiftScore());
        assertEquals(0.6, fundamental.dilutionRiskScore());
        assertEquals(0.8, fundamental.insiderAlignmentScore());

        assertEquals(1.0, options.callPutImbalanceScore());
        assertEquals(0.0, options.unusualActivityScore());
        assertEquals(0.4, options.openInterestExpansionScore());
        assertEquals(0.7, options.nearMoneyFlowScore());
        assertEquals(1.0, options.dealerPressureScore());

        assertEquals(0.0, propagation.mentionVelocityScore());
        assertEquals(0.3, propagation.propagationAccelerationScore());
        assertEquals(0.5, propagation.crossPlatformExpansionScore());
        assertEquals(1.0, propagation.sectorSympathyScore());
        assertEquals(0.9, propagation.narrativeCoherenceScore());
    }

    @Test
    void scoreOnlySnapshotsRejectNanScores() {
        assertThrows(IllegalArgumentException.class, () -> new LiquidityTextureSnapshot(
                Double.NaN, 0.2, 0.3, 0.4, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new IntradayStructureSnapshot(
                0.1, Double.NaN, 0.3, 0.4, 0.5, false));
        assertThrows(IllegalArgumentException.class, () -> new VolatilityExpansionSnapshot(
                0.1, 0.2, Double.NaN, 0.4, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new FundamentalImpactSnapshot(
                Double.NaN, 0.2, 0.3, 0.4, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new OptionsFlowSnapshot(
                Double.NaN, 0.2, 0.3, 0.4, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new PropagationSnapshot(
                Double.NaN, 0.2, 0.3, 0.4, 0.5));
    }
}
