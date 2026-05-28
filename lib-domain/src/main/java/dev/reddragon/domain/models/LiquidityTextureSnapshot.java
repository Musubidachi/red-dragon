package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized liquidity texture inputs — <b>score-only</b>, no symbol context.
 * Consumed by L4 scorers ({@code LiquidityTextureScorer}); derived from
 * {@link MarketLiquidityTextureSnapshot} before being passed to pure-function
 * scorers in {@code lib-analytics}.
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
        this.spreadQualityScore = DomainScorePolicy.clampDerivedScore(spreadQualityScore);
        this.orderBookDepthScore = DomainScorePolicy.clampDerivedScore(orderBookDepthScore);
        this.liquidityConsistencyScore = DomainScorePolicy.clampDerivedScore(liquidityConsistencyScore);
        this.slippageRiskScore = DomainScorePolicy.clampDerivedScore(slippageRiskScore);
        this.relativeVolumeScore = DomainScorePolicy.clampDerivedScore(relativeVolumeScore);
    }
}
