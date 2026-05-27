package dev.reddragon.analytics.services.propagation;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.domain.models.PropagationSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores reflexive narrative expansion quality.
 */
public class NarrativeExpansionScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(PropagationSnapshot propagation) {
        Objects.requireNonNull(propagation, "propagation is required");

        double score = AnalyticsScoreUtils.clamp(
                propagation.mentionVelocityScore() * 0.25
                        + propagation.propagationAccelerationScore() * 0.35
                        + propagation.crossPlatformExpansionScore() * 0.20
                        + propagation.narrativeCoherenceScore() * 0.20
        );

        List<String> notes = new ArrayList<>();
        addNote(score, notes);

        return new ScoreResult(score, notes);
    }

    private void addNote(double score, List<String> notes) {
        if (score >= 0.80) {
            notes.add("Narrative expansion is accelerating across multiple channels.");
            return;
        }

        if (score >= 0.55) {
            notes.add("Narrative expansion is visible but still developing.");
            return;
        }

        notes.add("Narrative expansion remains limited or fragmented.");
    }
}
