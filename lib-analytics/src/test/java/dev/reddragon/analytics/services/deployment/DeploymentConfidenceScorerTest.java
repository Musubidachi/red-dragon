package dev.reddragon.analytics.services.deployment;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boundary tests for the MD Layer 5 (Deployment) confidence scorer.
 */
class DeploymentConfidenceScorerTest {

    private static final Instant T0 = Instant.parse("2026-05-13T00:00:00Z");
    private final DeploymentConfidenceScorer scorer = new DeploymentConfidenceScorer();

    @Test
    void allInputsAtMaxHitsUpperBound() {
        double score = scorer.process(candidate(1.0, 1.0, 1.0), 1.0, 1.0, 1.0, 1.0, new ArrayList<>());
        assertEquals(1.0, score, 1e-9);
    }

    @Test
    void allInputsAtZeroHitsLowerBound() {
        double score = scorer.process(candidate(0.0, 0.0, 0.0), 0.0, 0.0, 0.0, 0.0, new ArrayList<>());
        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void emitsConcentratedNoteWhenStrong() {
        List<String> notes = new ArrayList<>();
        scorer.process(candidate(0.95, 0.9, 0.9), 0.95, 0.9, 0.9, 0.9, notes);
        assertTrue(notes.stream().anyMatch(n -> n.contains("concentrated review")),
                "strong inputs should produce a concentrated-review note");
    }

    @Test
    void emitsLimitedProbingNoteInMidRange() {
        List<String> notes = new ArrayList<>();
        scorer.process(candidate(0.50, 0.50, 0.50), 0.50, 0.50, 0.50, 0.50, notes);
        // 0.50 score lands in 0.45–0.65 (limited probing) or just above (standard).
        assertTrue(
                notes.stream().anyMatch(n -> n.contains("limited probing"))
                        || notes.stream().anyMatch(n -> n.contains("standard participation")),
                "mid-range inputs should produce probing or standard note"
        );
    }

    @Test
    void emitsWeakNoteWhenBelowProbeThreshold() {
        List<String> notes = new ArrayList<>();
        scorer.process(candidate(0.10, 0.10, 0.10), 0.10, 0.10, 0.10, 0.10, notes);
        assertTrue(notes.stream().anyMatch(n -> n.contains("weak relative to risk")),
                "low inputs should produce a weak note");
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
}
