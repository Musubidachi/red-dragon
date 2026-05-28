package dev.reddragon.analytics.services.structural;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.domain.models.FundamentalImpactSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores dilution and structural financing risk.
 */
public class DilutionRiskScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(FundamentalImpactSnapshot impact) {
        Objects.requireNonNull(impact, "impact is required");

        double score = AnalyticsScoreUtils.clamp(
                1.0 - impact.dilutionRiskScore()
        );

        List<String> notes = new ArrayList<>();
        addNote(score, notes);

        return new ScoreResult(score, notes);
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
