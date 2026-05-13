package dev.reddragon.analytics.structural;

import dev.reddragon.analytics.model.FundamentalImpactSnapshot;
import dev.reddragon.analytics.util.AnalyticsScoreUtils;

import java.util.List;
import java.util.Objects;

/**
 * Scores whether a catalyst is materially meaningful.
 */
public class MaterialityImpactScorer {

    /**
     * Main processing flow.
     */
    public double process(
            FundamentalImpactSnapshot impact,
            List<String> notes
    ) {
        Objects.requireNonNull(impact, "impact is required");
        Objects.requireNonNull(notes, "notes is required");

        double score = AnalyticsScoreUtils.clamp(
                impact.revenueImpactScore() * 0.30
                        + impact.marketCapRelativeImpactScore() * 0.30
                        + impact.structuralDemandShiftScore() * 0.30
                        + impact.insiderAlignmentScore() * 0.10
        );

        addNote(score, notes);

        return score;
    }

    private void addNote(double score, List<String> notes) {
        if (score >= 0.80) {
            notes.add("Catalyst appears materially significant relative to company scale.");
            return;
        }

        if (score >= 0.55) {
            notes.add("Catalyst appears meaningful but not transformative.");
            return;
        }

        notes.add("Catalyst appears weak relative to company scale or structure.");
    }
}
