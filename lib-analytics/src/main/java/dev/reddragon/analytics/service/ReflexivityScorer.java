package dev.reddragon.analytics.service;

import dev.reddragon.analytics.util.AnalyticsScoreUtils;
import dev.reddragon.ingestion.model.TradeCandidate;

import java.util.List;
import java.util.Objects;

/**
 * Scores probability that participation propagation amplifies disequilibrium.
 */
public class ReflexivityScorer {

    /**
     * Main processing flow.
     */
    public double process(
            TradeCandidate candidate,
            List<String> notes
    ) {
        Objects.requireNonNull(candidate, "candidate is required");
        Objects.requireNonNull(notes, "notes is required");

        double score = AnalyticsScoreUtils.clamp(
                candidate.reflexivityPotentialScore() * 0.60
                        + candidate.earlynessScore() * 0.40
        );

        addNotes(score, notes);

        return score;
    }

    private void addNotes(double score, List<String> notes) {
        if (score >= 0.75) {
            notes.add("Propagation dynamics suggest reflexive expansion is increasingly likely.");
            return;
        }

        if (score >= 0.50) {
            notes.add("Propagation dynamics are forming but not yet dominant.");
            return;
        }

        notes.add("Reflexive propagation remains weak or uncertain.");
    }
}
