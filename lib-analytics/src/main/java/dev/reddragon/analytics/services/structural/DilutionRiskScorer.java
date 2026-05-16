package dev.reddragon.analytics.services.structural;

import dev.reddragon.domain.models.FundamentalImpactSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.List;
import java.util.Objects;

/**
 * Scores dilution and structural financing risk.
 */
public class DilutionRiskScorer {

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
                1.0 - impact.dilutionRiskScore()
        );

        addNote(score, notes);

        return score;
    }

    private void addNote(double score, List<String> notes) {
        if (score >= 0.80) {
            notes.add("Dilution risk appears limited.");
            return;
        }

        if (score >= 0.55) {
            notes.add("Dilution risk exists but is manageable.");
            return;
        }

        notes.add("Dilution or financing risk appears elevated.");
    }
}
