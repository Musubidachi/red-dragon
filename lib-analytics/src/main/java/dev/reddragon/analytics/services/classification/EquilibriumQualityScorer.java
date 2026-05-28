package dev.reddragon.analytics.services.classification;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.math.AnalyticsScoreUtils;
import dev.reddragon.domain.models.MarketDataSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores whether market structure is usable for restoration or controlled expansion.
 */
public class EquilibriumQualityScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(MarketDataSnapshot marketData) {
        Objects.requireNonNull(marketData, "marketData is required");

        double liquidity = marketData.liquidityScore();
        double volatility = marketData.volatilityStabilityScore();
        double rangeBalance = rangeBalanceScore(marketData);
        List<String> notes = new ArrayList<>();

        addNotes(liquidity, volatility, rangeBalance, notes);

        double score = AnalyticsScoreUtils.clamp(
                liquidity * 0.35
                        + volatility * 0.40
                        + rangeBalance * 0.25
        );
        return new ScoreResult(score, notes);
    }

    private double rangeBalanceScore(MarketDataSnapshot marketData) {
        double distanceFromMidRange = Math.abs(marketData.rangePosition() - 0.50);
        return AnalyticsScoreUtils.clamp(1.0 - distanceFromMidRange * 2.0);
    }

    private void addNotes(
            double liquidity,
            double volatility,
            double rangeBalance,
            List<String> notes
    ) {
        if (liquidity < 0.35) {
            notes.add("Equilibrium quality is pressured by weak liquidity.");
        }

        if (volatility < 0.35) {
            notes.add("Equilibrium quality is pressured by unstable volatility.");
        }

        if (rangeBalance < 0.35) {
            notes.add("Equilibrium quality is pressured by extended range location.");
        }
    }
}
