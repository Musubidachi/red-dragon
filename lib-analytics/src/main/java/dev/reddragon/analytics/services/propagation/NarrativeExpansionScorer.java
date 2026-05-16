package dev.reddragon.analytics.services.propagation;

import dev.reddragon.domain.models.PropagationSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.List;
import java.util.Objects;

/**
 * Scores reflexive narrative expansion quality.
 */
public class NarrativeExpansionScorer {

    /**
     * Main processing flow.
     */
    public double process(
            PropagationSnapshot propagation,
            List<String> notes
    ) {
        Objects.requireNonNull(propagation, "propagation is required");
        Objects.requireNonNull(notes, "notes is required");

        double score = AnalyticsScoreUtils.clamp(
                propagation.mentionVelocityScore() * 0.25
                        + propagation.propagationAccelerationScore() * 0.35
                        + propagation.crossPlatformExpansionScore() * 0.20
                        + propagation.narrativeCoherenceScore() * 0.20
        );

        addNote(score, notes);

        return score;
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
