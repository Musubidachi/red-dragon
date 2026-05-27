package dev.reddragon.analytics.services.classification;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.domain.models.*;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationScorersBoundaryTest {

    @Test
    void optionsFlowScorerPinsLowAndHigh() {
        OptionsFlowScorer scorer = new OptionsFlowScorer();

        ScoreResult low = scorer.process(new OptionsFlowSnapshot(0,0,0,0,0));
        ScoreResult high = scorer.process(new OptionsFlowSnapshot(1,1,1,1,1));

        assertEquals(0.0, low.score());
        assertEquals(1.0, high.score());
        assertTrue(low.notes().stream().anyMatch(n -> n.contains("weak")));
        assertTrue(high.notes().stream().anyMatch(n -> n.contains("supportive")));
    }

    @Test
    void vwapInteractionScorerRewardsTightConstructiveReclaim() {
        VwapInteractionScorer scorer = new VwapInteractionScorer();
        double best = scorer.process(new IntradayStructureSnapshot(0.005,1.0,0.3,0.9,0.7,true)).score();
        double weak = scorer.process(new IntradayStructureSnapshot(0.12,0.1,0.8,0.2,0.2,false)).score();
        assertTrue(best > weak);
        assertTrue(best >= 0.75);
    }

    @Test
    void directionalPersistenceScorerPenalizesHighPersistence() {
        DirectionalPersistenceScorer scorer = new DirectionalPersistenceScorer();
        double supportive = scorer.process(new IntradayStructureSnapshot(0.01,0.5,0.1,0.9,0.2,true)).score();
        double hostile = scorer.process(new IntradayStructureSnapshot(0.01,0.5,1.0,0.1,0.9,true)).score();
        assertTrue(supportive > hostile);
    }

    @Test
    void liquidityTextureScorerPinsLowAndHigh() {
        LiquidityTextureScorer scorer = new LiquidityTextureScorer();
        double low = scorer.process(new LiquidityTextureSnapshot(0,0,0,1,0)).score();
        double high = scorer.process(new LiquidityTextureSnapshot(1,1,1,0,1)).score();
        assertEquals(0.0, low);
        assertEquals(1.0, high);
    }

    @Test
    void volatilityExpansionScorerHandlesAtrBuckets() {
        VolatilityExpansionScorer scorer = new VolatilityExpansionScorer();
        double stable = scorer.process(new VolatilityExpansionSnapshot(1.0,1.0,0.1,0.9,0.9)).score();
        double unstable = scorer.process(new VolatilityExpansionSnapshot(3.0,1.0,0.9,0.2,0.2)).score();
        assertTrue(stable > unstable);
    }

    @Test
    void regimeCompatibilityClassificationAndScoreBoundaries() {
        RegimeCompatibilityScorer scorer = new RegimeCompatibilityScorer();

        RegimeCompatibilityResult hostileLiquidity = scorer.process(snapshot(0.2,0.8,0.5));
        RegimeCompatibilityResult supportiveCompression = scorer.process(snapshot(0.8,0.8,0.5));

        assertEquals(RegimeLabel.HOSTILE_LIQUIDITY, hostileLiquidity.regimeLabel());
        assertEquals(RegimeLabel.SUPPORTIVE_COMPRESSION, supportiveCompression.regimeLabel());
        assertTrue(hostileLiquidity.notes().stream().anyMatch(n -> n.contains("Liquidity is weak")));
        assertTrue(supportiveCompression.notes().stream().anyMatch(n -> n.contains("tightly balanced")));
        assertEquals(0.20, scorer.score(RegimeLabel.HOSTILE_LIQUIDITY));
        assertEquals(0.72, scorer.score(RegimeLabel.SUPPORTIVE_COMPRESSION));
    }

    @Test
    void equilibriumQualityScorerPenalizesRangeExtension() {
        EquilibriumQualityScorer scorer = new EquilibriumQualityScorer();
        double balanced = scorer.process(snapshot(0.8,0.8,0.5)).score();
        ScoreResult extended = scorer.process(snapshot(0.8,0.8,0.98));
        assertTrue(balanced > extended.score());
        assertTrue(extended.notes().stream().anyMatch(n -> n.contains("extended range")));
    }

    private MarketDataSnapshot snapshot(double liquidity, double volatility, double rangePosition) {
        return new MarketDataSnapshot("ABC", Instant.parse("2026-05-13T00:00:00Z"), 10, 9.8, 0.02, 0.5,
                rangePosition, 1_000_000, liquidity, volatility, 1.2, 0.0, 0.5,
                MarketDataQuality.COMPLETE, List.of());
    }
}
