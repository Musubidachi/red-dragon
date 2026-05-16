package dev.reddragon.domain.models;

import dev.reddragon.math.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized volatility expansion inputs supplied by market-data adapters.
 */
@Value
@Accessors(fluent = true)
public class VolatilityExpansionSnapshot {
    double currentAtrPercent;
    double baselineAtrPercent;
    double impliedVolatilityRankScore;
    double realizedVolatilityExpansionScore;
    double volatilityCompressionScore;

    public VolatilityExpansionSnapshot(
            double currentAtrPercent,
            double baselineAtrPercent,
            double impliedVolatilityRankScore,
            double realizedVolatilityExpansionScore,
            double volatilityCompressionScore
    ) {
        this.currentAtrPercent = currentAtrPercent;
        this.baselineAtrPercent = baselineAtrPercent;
        this.impliedVolatilityRankScore = AnalyticsScoreUtils.clamp(impliedVolatilityRankScore);
        this.realizedVolatilityExpansionScore = AnalyticsScoreUtils.clamp(realizedVolatilityExpansionScore);
        this.volatilityCompressionScore = AnalyticsScoreUtils.clamp(volatilityCompressionScore);
    }
}
