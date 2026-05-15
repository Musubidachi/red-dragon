package dev.reddragon.analytics.services.propagation;

import dev.reddragon.analytics.models.PropagationSnapshot;
import dev.reddragon.analytics.utilities.AnalyticsScoreUtils;

import java.util.List;
import java.util.Objects;

/**
 * Scores whether propagation is spreading through adjacent tickers or sector peers.
 */
public class SectorPropagationScorer {

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
                propagation.sectorSympathyScore() * 0.45
                        + propagation.crossPlatformExpansionScore() * 0.25
                        + propagation.mentionVelocityScore() * 0.15
                        + propagation.narrativeCoherenceScore() * 0.15
        );

        addNote(score, notes);

        return score;
    }

    private void addNote(double score, List<String> notes) {
        if (score >= 0.75) {
            notes.add("Sector propagation is strong; adjacent participation may reinforce the move.");
            return;
        }

        if (score >= 0.50) {
            notes.add("Sector propagation is forming but not yet dominant.");
            return;
        }

        notes.add("Sector propagation is weak; the thesis appears isolated for now.");
    }
}
