package dev.reddragon.analytics.classification;

import dev.reddragon.analytics.model.*;
import dev.reddragon.marketdata.model.MarketDataQuality;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationScorersBoundaryTest {

    @Test
    void optionsFlowScorerPinsLowAndHigh() {
        OptionsFlowScorer scorer = new OptionsFlowScorer();
        List<String> notes = new ArrayList<>();

        double low = scorer.process(new OptionsFlowSnapshot(0,0,0,0,0), notes);
        double high = scorer.process(new OptionsFlowSnapshot(1,1,1,1,1), notes);

        assertEquals(0.0, low);
        assertEquals(1.0, high);
    }

    @Test
    void vwapInteractionScorerRewardsTightConstructiveReclaim() {
        VwapInteractionScorer scorer = new VwapInteractionScorer();
        double best = scorer.process(new IntradayStructureSnapshot(0.005,1.0,0.3,0.9,0.7,true), new ArrayList<>());
        double weak = scorer.process(new IntradayStructureSnapshot(0.12,0.1,0.8,0.2,0.2,false), new ArrayList<>());
        assertTrue(best > weak);
        assertTrue(best >= 0.75);
    }

    @Test
    void directionalPersistenceScorerPenalizesHighPersistence() {
        DirectionalPersistenceScorer scorer = new DirectionalPersistenceScorer();
        double supportive = scorer.process(new IntradayStructureSnapshot(0.01,0.5,0.1,0.9,0.2,true), new ArrayList<>());
        double hostile = scorer.process(new IntradayStructureSnapshot(0.01,0.5,1.0,0.1,0.9,true), new ArrayList<>());
        assertTrue(supportive > hostile);
    }

    @Test
    void liquidityTextureScorerPinsLowAndHigh() {
        LiquidityTextureScorer scorer = new LiquidityTextureScorer();
        double low = scorer.process(new LiquidityTextureSnapshot(0,0,0,1,0), new ArrayList<>());
        double high = scorer.process(new LiquidityTextureSnapshot(1,1,1,0,1), new ArrayList<>());
        assertEquals(0.0, low);
        assertEquals(1.0, high);
    }

    @Test
    void volatilityExpansionScorerHandlesAtrBuckets() {
        VolatilityExpansionScorer scorer = new VolatilityExpansionScorer();
        double stable = scorer.process(new VolatilityExpansionSnapshot(1.0,1.0,0.1,0.9,0.9), new ArrayList<>());
        double unstable = scorer.process(new VolatilityExpansionSnapshot(3.0,1.0,0.9,0.2,0.2), new ArrayList<>());
        assertTrue(stable > unstable);
    }

    @Test
    void regimeCompatibilityClassificationAndScoreBoundaries() {
        RegimeCompatibilityScorer scorer = new RegimeCompatibilityScorer();

        RegimeLabel hostileLiquidity = scorer.process(snapshot(0.2,0.8,0.5), new ArrayList<>());
        RegimeLabel supportiveRotation = scorer.process(snapshot(0.8,0.8,0.5), new ArrayList<>());

        assertEquals(RegimeLabel.HOSTILE_LIQUIDITY, hostileLiquidity);
        assertEquals(RegimeLabel.SUPPORTIVE_ROTATIONAL, supportiveRotation);
        assertEquals(0.20, scorer.score(RegimeLabel.HOSTILE_LIQUIDITY));
        assertEquals(0.85, scorer.score(RegimeLabel.SUPPORTIVE_ROTATIONAL));
    }

    @Test
    void equilibriumQualityScorerPenalizesRangeExtension() {
        EquilibriumQualityScorer scorer = new EquilibriumQualityScorer();
        double balanced = scorer.process(snapshot(0.8,0.8,0.5), new ArrayList<>());
        double extended = scorer.process(snapshot(0.8,0.8,0.98), new ArrayList<>());
        assertTrue(balanced > extended);
    }

    private MarketDataSnapshot snapshot(double liquidity, double volatility, double rangePosition) {
        return new MarketDataSnapshot("ABC", Instant.parse("2026-05-13T00:00:00Z"), 10, 9.8, 0.02, 0.5,
                rangePosition, 1_000_000, liquidity, volatility, 1.2, 0.0, 0.5,
                MarketDataQuality.COMPLETE, List.of());
    }
}
