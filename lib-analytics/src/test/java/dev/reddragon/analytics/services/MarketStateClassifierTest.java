package dev.reddragon.analytics.services;

import dev.reddragon.domain.models.MarketIntradayStructureSnapshot;
import dev.reddragon.domain.models.MarketStateSignal;
import dev.reddragon.domain.models.RegimeLabel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for MarketStateClassifier - the standalone classifier exposed at
 * POST /api/market-state/classify (per the lib-analytics README).
 */
class MarketStateClassifierTest {

    private final MarketStateClassifier classifier = new MarketStateClassifier();

    @Test
    void nullSnapshotRejected() {
        assertThrows(NullPointerException.class, () -> classifier.process(null));
    }

    @Test
    void highRotationalLowPersistenceLabelsSupportiveRotational() {
        // rotational=0.80, persistence=0.30, trend=0.40, reclaim=0.50
        MarketIntradayStructureSnapshot s = snapshot(0.80, 0.30, 0.40, 0.50, 0.5);
        MarketStateSignal signal = classifier.process(s);
        assertEquals(RegimeLabel.SUPPORTIVE_ROTATIONAL, signal.regimeLabel());
        assertTrue(signal.deploymentSupported(), "supportive rotational regime should support deployment");
    }

    @Test
    void highPersistenceAndTrendLabelsSupportiveTrend() {
        MarketIntradayStructureSnapshot s = snapshot(0.30, 0.85, 0.80, 0.50, 0.5);
        MarketStateSignal signal = classifier.process(s);
        assertEquals(RegimeLabel.SUPPORTIVE_TREND, signal.regimeLabel());
    }

    @Test
    void weakReclaimWithExtendedDisplacementLabelsHostileVolatility() {
        MarketIntradayStructureSnapshot s = snapshot(0.40, 0.40, 0.40, 0.15, 3.0);
        MarketStateSignal signal = classifier.process(s);
        assertEquals(RegimeLabel.HOSTILE_VOLATILITY, signal.regimeLabel());
        assertEquals(false, signal.deploymentSupported(), "hostile regime should NOT support deployment");
    }

    @Test
    void allWeakSignalsLabelHostileLiquidity() {
        MarketIntradayStructureSnapshot s = snapshot(0.15, 0.20, 0.20, 0.50, 0.5);
        MarketStateSignal signal = classifier.process(s);
        assertEquals(RegimeLabel.HOSTILE_LIQUIDITY, signal.regimeLabel());
    }

    @Test
    void mixedSignalsLabelMixed() {
        MarketIntradayStructureSnapshot s = snapshot(0.50, 0.50, 0.50, 0.50, 0.5);
        MarketStateSignal signal = classifier.process(s);
        assertEquals(RegimeLabel.MIXED, signal.regimeLabel());
    }

    @Test
    void restorationProbabilityIsAlwaysInZeroOneRange() {
        MarketIntradayStructureSnapshot s = snapshot(0.80, 0.30, 0.40, 0.50, 0.5);
        MarketStateSignal signal = classifier.process(s);
        assertTrue(signal.equilibriumRestorationProbability() >= 0.0
                && signal.equilibriumRestorationProbability() <= 1.0);
    }

    private MarketIntradayStructureSnapshot snapshot(
            double rotational, double persistence, double trend, double reclaim, double vwapDistancePercent
    ) {
        return new MarketIntradayStructureSnapshot(
                "ACME", 100.0, 100.0, vwapDistancePercent,
                reclaim, persistence, rotational, trend, true
        );
    }
}
