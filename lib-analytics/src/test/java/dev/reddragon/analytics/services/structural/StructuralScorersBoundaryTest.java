package dev.reddragon.analytics.services.structural;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boundary tests for the MD Layer 3 (Structural Validation) scorers.
 *
 * <p>Each scorer is exercised at one or both of its endpoints (0.0 / 1.0)
 * plus at least one penalty path so weight changes can't silently drift.
 */
class StructuralScorersBoundaryTest {

    private static final Instant T0 = Instant.parse("2026-05-13T00:00:00Z");

    @Test
    void asymmetryScorerHitsUpperBoundWithCleanInputs() {
        AsymmetryScorer scorer = new AsymmetryScorer();
        double score = scorer.process(
                candidate(1.0, 1.0, 1.0),
                snapshot(0.50, 0.04, 1.0, 1.0),
                /* equilibriumQuality = */ 1.0,
                new ArrayList<>()
        );
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void asymmetryScorerHitsLowerBoundWithZeroInputs() {
        AsymmetryScorer scorer = new AsymmetryScorer();
        double score = scorer.process(
                candidate(0.0, 0.0, 0.0),
                snapshot(0.50, 0.04, 0.0, 0.0),
                /* equilibriumQuality = */ 0.0,
                new ArrayList<>()
        );
        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void asymmetryScorerAppliesRangePenaltyWhenExtended() {
        AsymmetryScorer scorer = new AsymmetryScorer();
        List<String> notes = new ArrayList<>();
        double balanced = scorer.process(
                candidate(0.8, 0.8, 0.8),
                snapshot(0.50, 0.04, 1.0, 1.0),
                0.8,
                new ArrayList<>()
        );
        double extended = scorer.process(
                candidate(0.8, 0.8, 0.8),
                snapshot(0.92, 0.04, 1.0, 1.0),
                0.8,
                notes
        );
        assertTrue(balanced > extended, "extended range must compress asymmetry");
        assertTrue(notes.stream().anyMatch(n -> n.contains("Range position is extended")),
                "range penalty must produce a note");
    }

    @Test
    void asymmetryScorerAppliesGapPenaltyWhenLargeGap() {
        AsymmetryScorer scorer = new AsymmetryScorer();
        List<String> notes = new ArrayList<>();
        double clean = scorer.process(
                candidate(0.8, 0.8, 0.8),
                snapshot(0.50, 0.04, 1.0, 1.0),
                0.8,
                new ArrayList<>()
        );
        double gappedUp = scorer.process(
                candidate(0.8, 0.8, 0.8),
                snapshot(0.50, 0.20, 1.0, 1.0),
                0.8,
                notes
        );
        assertTrue(clean > gappedUp, "large gap must compress asymmetry");
        assertTrue(notes.stream().anyMatch(n -> n.contains("Large gap detected")),
                "gap penalty must produce a note");
    }

    private TradeCandidate candidate(double structural, double material, double earlyness) {
        return new TradeCandidate(
                "test-id", "ABC", "ACME Corp",
                CandidateCatalystType.FILING_EVENT, SourceType.SEC_EDGAR,
                "acc-1", "https://example.com/doc",
                T0, "headline", "summary",
                structural, material, earlyness, /* reflexivity */ 0.5
        );
    }

    private MarketDataSnapshot snapshot(double rangePosition, double gapPercent, double liquidity, double volatility) {
        return new MarketDataSnapshot(
                "ABC", T0, 10.0, 9.8, gapPercent, 0.20, rangePosition,
                1_000_000, liquidity, volatility, 1.2, 0.0, 0.5,
                MarketDataQuality.COMPLETE, List.of()
        );
    }
}
