package dev.reddragon.domain.models;

import dev.reddragon.math.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized volatility expansion inputs — <b>score-only</b>, no symbol
 * context. Consumed by L4 scorers ({@code VolatilityExpansionScorer});
 * derived from {@link MarketVolatilityExpansionSnapshot} before being passed
 * to pure-function scorers in {@code lib-analytics}.
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
