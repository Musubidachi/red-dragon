package dev.reddragon.marketdata.model;

import dev.reddragon.marketdata.util.MarketMathUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Derived volatility expansion metrics produced by marketdata calculators.
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
