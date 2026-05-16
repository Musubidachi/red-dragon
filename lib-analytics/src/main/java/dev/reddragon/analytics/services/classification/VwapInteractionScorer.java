package dev.reddragon.analytics.services.classification;

import dev.reddragon.domain.models.IntradayStructureSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.List;
import java.util.Objects;

/**
 * Scores whether price is interacting constructively with VWAP.
 */
public class VwapInteractionScorer {

    /**
     * Main processing flow.
     */
    public double process(
            IntradayStructureSnapshot intraday,
            List<String> notes
    ) {
        Objects.requireNonNull(intraday, "intraday is required");
        Objects.requireNonNull(notes, "notes is required");

        double distanceScore = distanceScore(intraday.vwapDistancePercent());
        double reclaimScore = intraday.vwapReclaimStrength();
        double locationScore = locationScore(intraday.aboveVwap());

        double score = AnalyticsScoreUtils.clamp(
                distanceScore * 0.35
                        + reclaimScore * 0.45
                        + locationScore * 0.20
        );

        addNote(score, intraday, notes);

        return score;
    }

    private double distanceScore(double vwapDistancePercent) {
        double absoluteDistance = Math.abs(vwapDistancePercent);

        if (absoluteDistance <= 0.01) {
            return 1.0;
        }

        if (absoluteDistance <= 0.03) {
            return 0.80;
        }

        if (absoluteDistance <= 0.06) {
            return 0.55;
        }

        if (absoluteDistance <= 0.10) {
            return 0.30;
        }

        return 0.15;
    }

    private double locationScore(boolean aboveVwap) {
        return aboveVwap ? 0.75 : 0.45;
    }

    private void addNote(
            double score,
            IntradayStructureSnapshot intraday,
            List<String> notes
    ) {
        if (score >= 0.75) {
            notes.add("VWAP behavior is constructive; price is interacting with VWAP in a controlled way.");
            return;
        }

        if (score >= 0.50) {
            notes.add("VWAP behavior is mixed but not hostile.");
            return;
        }

        notes.add("VWAP behavior is weak or extended; equilibrium quality may be degraded.");
    }
}
