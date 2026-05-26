package dev.reddragon.domain.models;

import dev.reddragon.math.MarketMathUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Derived volatility expansion metrics <b>with provider context</b>: symbol
 * plus raw and normalized ATR fields. Output of {@code lib-marketdata}'s
 * {@code VolatilityExpansionSnapshotBuilder}. See
 * {@link VolatilityExpansionSnapshot} for the trimmed score-only shape that
 * lib-analytics scorers consume.
 */
@Value
@Accessors(fluent = true)
public class MarketVolatilityExpansionSnapshot {
    String symbol;
    double currentAtr;
    double baselineAtr;
    double realizedVolatility;
    double volatilityExpansionScore;
    double volatilityCompressionScore;

    public MarketVolatilityExpansionSnapshot(
            String symbol,
            double currentAtr,
            double baselineAtr,
            double realizedVolatility,
            double volatilityExpansionScore,
            double volatilityCompressionScore
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }

        this.symbol = symbol.trim().toUpperCase();
        this.currentAtr = currentAtr;
        this.baselineAtr = baselineAtr;
        this.realizedVolatility = realizedVolatility;
        this.volatilityExpansionScore = MarketMathUtils.clamp(volatilityExpansionScore);
        this.volatilityCompressionScore = MarketMathUtils.clamp(volatilityCompressionScore);
    }
}
