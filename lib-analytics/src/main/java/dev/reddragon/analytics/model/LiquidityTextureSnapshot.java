package dev.reddragon.analytics.model;

import dev.reddragon.analytics.util.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized liquidity texture inputs.
 */
@Value
@Accessors(fluent = true)
public class LiquidityTextureSnapshot {
    double spreadQualityScore;
    double orderBookDepthScore;
    double liquidityConsistencyScore;
    double slippageRiskScore;
    double relativeVolumeScore;

    public LiquidityTextureSnapshot(
            double spreadQualityScore,
            double orderBookDepthScore,
            double liquidityConsistencyScore,
            double slippageRiskScore,
            double relativeVolumeScore
    ) {
        this.spreadQualityScore = AnalyticsScoreUtils.clamp(spreadQualityScore);
        this.orderBookDepthScore = AnalyticsScoreUtils.clamp(orderBookDepthScore);
        this.liquidityConsistencyScore = AnalyticsScoreUtils.clamp(liquidityConsistencyScore);
        this.slippageRiskScore = AnalyticsScoreUtils.clamp(slippageRiskScore);
        this.relativeVolumeScore = AnalyticsScoreUtils.clamp(relativeVolumeScore);
    }
}
