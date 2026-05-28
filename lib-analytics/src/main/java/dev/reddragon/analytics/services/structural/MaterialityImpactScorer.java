package dev.reddragon.analytics.services.structural;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.domain.models.FundamentalImpactSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores whether a catalyst is materially meaningful.
 */
public class MaterialityImpactScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(FundamentalImpactSnapshot impact) {
        Objects.requireNonNull(impact, "impact is required");

        double score = AnalyticsScoreUtils.clamp(
                impact.revenueImpactScore() * 0.30
                        + impact.marketCapRelativeImpactScore() * 0.30
                        + impact.structuralDemandShiftScore() * 0.30
                        + impact.insiderAlignmentScore() * 0.10
        );

        List<String> notes = new ArrayList<>();
        addNote(score, notes);

        return new ScoreResult(score, notes);
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
