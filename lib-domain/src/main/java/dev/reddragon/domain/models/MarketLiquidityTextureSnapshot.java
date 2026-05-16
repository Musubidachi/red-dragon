package dev.reddragon.domain.models;

import dev.reddragon.math.MarketMathUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Derived liquidity texture metrics produced by marketdata calculators.
 */
@Value
@Accessors(fluent = true)
public class MarketLiquidityTextureSnapshot {
    String symbol;
    double spreadQualityScore;
    double orderBookDepthScore;
    double liquidityConsistencyScore;
    double slippageRiskScore;
    double relativeVolumeScore;

    public MarketLiquidityTextureSnapshot(
            String symbol,
            double spreadQualityScore,
            double orderBookDepthScore,
            double liquidityConsistencyScore,
            double slippageRiskScore,
            double relativeVolumeScore
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }

        this.symbol = symbol.trim().toUpperCase();
        this.spreadQualityScore = MarketMathUtils.clamp(spreadQualityScore);
        this.orderBookDepthScore = MarketMathUtils.clamp(orderBookDepthScore);
        this.liquidityConsistencyScore = MarketMathUtils.clamp(liquidityConsistencyScore);
        this.slippageRiskScore = MarketMathUtils.clamp(slippageRiskScore);
        this.relativeVolumeScore = MarketMathUtils.clamp(relativeVolumeScore);
    }
}
