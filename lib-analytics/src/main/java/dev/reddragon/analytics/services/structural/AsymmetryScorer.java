package dev.reddragon.analytics.services.structural;

import dev.reddragon.math.AnalyticsScoreUtils;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketDataSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * Scores remaining payoff asymmetry after market adaptation.
 */
public class AsymmetryScorer {

    /**
     * Main processing flow.
     */
    public double process(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            double equilibriumQuality,
            List<String> notes
    ) {
        Objects.requireNonNull(candidate, "candidate is required");
        Objects.requireNonNull(marketData, "marketData is required");
        Objects.requireNonNull(notes, "notes is required");

        double rangePenalty = rangePenalty(marketData, notes);
        double gapPenalty = gapPenalty(marketData, notes);

        return AnalyticsScoreUtils.clamp(
                candidate.structuralRealityScore() * 0.25
                        + candidate.materialSignificanceScore() * 0.25
                        + candidate.earlynessScore() * 0.25
                        + equilibriumQuality * 0.25
                        - rangePenalty
                        - gapPenalty
        );
    }

    private double rangePenalty(
            MarketDataSnapshot marketData,
            List<String> notes
    ) {
        if (marketData.rangePosition() > 0.85) {
            notes.add("Range position is extended; remaining asymmetry may be compressed.");
            return 0.20;
        }

        return 0.0;
    }

    private double gapPenalty(
            MarketDataSnapshot marketData,
            List<String> notes
    ) {
        if (Math.abs(marketData.gapPercent()) > 0.12) {
            notes.add("Large gap detected; entry asymmetry may be degraded.");
            return 0.15;
        }

        return 0.0;
    }
}
