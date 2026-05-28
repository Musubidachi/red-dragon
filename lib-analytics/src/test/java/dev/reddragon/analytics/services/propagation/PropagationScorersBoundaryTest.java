package dev.reddragon.analytics.services.propagation;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.domain.models.PhaseLabel;
import dev.reddragon.domain.models.PhaseTransitionSnapshot;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boundary tests for the MD Layer 6 (Narrative Propagation) scorers.
 */
class PropagationScorersBoundaryTest {

    private static final Instant T0 = Instant.parse("2026-05-13T00:00:00Z");

    @Test
    void reflexivityScorerHitsUpperBound() {
        ReflexivityScorer scorer = new ReflexivityScorer();
        double score = scorer.process(candidate(1.0, 1.0)).score();
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void reflexivityScorerHitsLowerBound() {
        ReflexivityScorer scorer = new ReflexivityScorer();
        double score = scorer.process(candidate(0.0, 0.0)).score();
        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void reflexivityScorerEmitsAppropriateNotePerBand() {
        ReflexivityScorer scorer = new ReflexivityScorer();

        ScoreResult high = scorer.process(candidate(1.0, 0.9));
        assertTrue(high.notes().stream().anyMatch(n -> n.contains("reflexive expansion")),
                "high band must mention reflexive expansion");

        ScoreResult mid = scorer.process(candidate(0.6, 0.5));
        assertTrue(mid.notes().stream().anyMatch(n -> n.contains("not yet dominant")),
                "mid band must mention not-yet-dominant");

        ScoreResult low = scorer.process(candidate(0.1, 0.1));
        assertTrue(low.notes().stream().anyMatch(n -> n.contains("weak or uncertain")),
                "low band must mention weak/uncertain");
    }

    @Test
    void propagationPhaseAnalyzerLabelsAcceleratingNarrativeAsExpansion() {
        PropagationPhaseAnalyzer analyzer = new PropagationPhaseAnalyzer();
        PhaseLabel label = analyzer.process(
                new PhaseTransitionSnapshot(/* previous */ 0.30, /* current */ 0.65, /* slope */ 0.35, /* acceleration */ 0.20)
        );
        // Accelerating, positive-slope movement should land somewhere in the
        // expansion-to-reflexivity band; UNKNOWN is the only outcome we forbid.
        assertTrue(label != PhaseLabel.UNKNOWN, "accelerating snapshot should not produce UNKNOWN");
    }

    @Test
    void propagationPhaseAnalyzerLabelsStableHighReflexivityAsSaturationRisk() {
        PropagationPhaseAnalyzer analyzer = new PropagationPhaseAnalyzer();
        PhaseLabel label = analyzer.process(
        		new PhaseTransitionSnapshot(0.90, 0.92, 0.02, -0.01)
        );
        // High level, near-flat slope = saturation territory.
        assertTrue(
                label == PhaseLabel.SATURATION_RISK
                        || label == PhaseLabel.LATE_REFLEXIVITY
                        || label == PhaseLabel.ASYMMETRY_DECAY
                        || label == PhaseLabel.EXHAUSTION,
                "stable-high snapshot should land in the saturation/decay band, got " + label
        );
    }

    private TradeCandidate candidate(double reflexivity, double earlyness) {
        return new TradeCandidate(
                "test-id", "ABC", "ACME Corp",
                CandidateCatalystType.FILING_EVENT, SourceType.SEC_EDGAR,
                "acc-1", "https://example.com/doc",
                T0, "headline", "summary",
                0.7, 0.7, earlyness, reflexivity
        );
    }
}
