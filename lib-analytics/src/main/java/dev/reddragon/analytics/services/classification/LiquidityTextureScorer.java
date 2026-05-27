package dev.reddragon.analytics.services.classification;

import dev.reddragon.analytics.services.ScoreResult;
import dev.reddragon.domain.models.LiquidityTextureSnapshot;
import dev.reddragon.math.AnalyticsScoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores liquidity quality and execution texture.
 */
public class LiquidityTextureScorer {

    /**
     * Main processing flow.
     */
    public ScoreResult process(LiquidityTextureSnapshot liquidity) {
        Objects.requireNonNull(liquidity, "liquidity is required");

        double score = AnalyticsScoreUtils.clamp(
                liquidity.spreadQualityScore() * 0.25
                        + liquidity.orderBookDepthScore() * 0.25
                        + liquidity.liquidityConsistencyScore() * 0.20
                        + (1.0 - liquidity.slippageRiskScore()) * 0.15
                        + liquidity.relativeVolumeScore() * 0.15
        );

        List<String> notes = new ArrayList<>();
        addNote(score, notes);

        return new ScoreResult(score, notes);
    }

    private void addNote(double score, List<String> notes) {
        if (score >= 0.75) {
            notes.add("Liquidity texture appears healthy and execution-friendly.");
            return;
        }

        if (score >= 0.50) {
            notes.add("Liquidity texture is usable but inconsistent.");
            return;
        }

        notes.add("Liquidity texture is hostile; slippage and instability risk are elevated.");
    }
}
