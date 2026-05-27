package dev.reddragon.analytics.services.propagation;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.math.AnalyticsScoreUtils;
import dev.reddragon.domain.models.TradeCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores probability that participation propagation amplifies disequilibrium.
 */
public class ReflexivityScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(TradeCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate is required");

        double score = AnalyticsScoreUtils.clamp(
                candidate.reflexivityPotentialScore() * 0.60
                        + candidate.earlynessScore() * 0.40
        );

        List<String> notes = new ArrayList<>();
        addNotes(score, notes);

        return new ScoreResult(score, notes);
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
