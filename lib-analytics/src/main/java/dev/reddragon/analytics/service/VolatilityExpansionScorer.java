package dev.reddragon.analytics.service;

import dev.reddragon.analytics.model.VolatilityExpansionSnapshot;
import dev.reddragon.analytics.util.AnalyticsScoreUtils;

import java.util.List;
import java.util.Objects;

/**
 * Scores volatility expansion stability and compression behavior.
 */
public class VolatilityExpansionScorer {

    /**
     * Main processing flow.
     */
    public double process(
            VolatilityExpansionSnapshot volatility,
            List<String> notes
    ) {
        Objects.requireNonNull(volatility, "volatility is required");
        Objects.requireNonNull(notes, "notes is required");

        double atrRatio = atrRatio(volatility);
        double expansionBalance = expansionBalance(volatility);

        double score = AnalyticsScoreUtils.clamp(
                atrRatio * 0.25
                        + expansionBalance * 0.35
                        + volatility.volatilityCompressionScore() * 0.20
                        + (1.0 - volatility.impliedVolatilityRankScore()) * 0.20
        );

        addNote(score, notes);

        return score;
    }

    private double atrRatio(VolatilityExpansionSnapshot volatility) {
        if (volatility.baselineAtrPercent() <= 0.0) {
            return 0.50;
        }

        double ratio = volatility.currentAtrPercent() / volatility.baselineAtrPercent();

        if (ratio <= 1.2) {
            return 0.90;
        }

        if (ratio <= 1.8) {
            return 0.65;
        }

        if (ratio <= 2.5) {
            return 0.40;
        }

        return 0.20;
    }

    private double expansionBalance(VolatilityExpansionSnapshot volatility) {
        return 1.0 - Math.abs(
                volatility.realizedVolatilityExpansionScore()
                        - volatility.volatilityCompressionScore()
        );
    }

    private void addNote(double score, List<String> notes) {
        if (score >= 0.75) {
            notes.add("Volatility structure remains controlled and supportive.");
            return;
        }

        if (score >= 0.50) {
            notes.add("Volatility structure is elevated but manageable.");
            return;
        }

        notes.add("Volatility expansion appears unstable or disorderly.");
    }
}
