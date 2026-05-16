package dev.reddragon.analytics.services.classification;

import dev.reddragon.domain.models.RegimeLabel;
import dev.reddragon.domain.models.MarketDataSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * Classifies broad market compatibility from liquidity, volatility, and range behavior.
 */
public class RegimeCompatibilityScorer {

    /**
     * Main processing flow.
     */
    public RegimeLabel process(
            MarketDataSnapshot marketData,
            List<String> notes
    ) {
        Objects.requireNonNull(marketData, "marketData is required");
        Objects.requireNonNull(notes, "notes is required");

        if (hostileLiquidity(marketData)) {
            notes.add("Liquidity is weak; regime is hostile to concentration.");
            return RegimeLabel.HOSTILE_LIQUIDITY;
        }

        if (hostileVolatility(marketData)) {
            notes.add("Volatility is unstable; equilibrium behavior is degraded.");
            return RegimeLabel.HOSTILE_VOLATILITY;
        }

        if (supportiveRotation(marketData)) {
            notes.add("Range position is balanced enough for rotational/restoration behavior.");
            return RegimeLabel.SUPPORTIVE_ROTATIONAL;
        }

        if (supportiveTrend(marketData)) {
            notes.add("Trend pressure is present but volatility remains controlled.");
            return RegimeLabel.SUPPORTIVE_TREND;
        }

        notes.add("Market regime is mixed; no hard support or rejection from market structure alone.");
        return RegimeLabel.MIXED;
    }

    public double score(RegimeLabel regimeLabel) {
        return switch (regimeLabel) {
            case SUPPORTIVE_ROTATIONAL -> 0.85;
            case SUPPORTIVE_TREND -> 0.70;
            case MIXED -> 0.50;
            case HOSTILE_VOLATILITY -> 0.25;
            case HOSTILE_LIQUIDITY -> 0.20;
            default -> 0.00;
        };
    }

    private boolean hostileLiquidity(MarketDataSnapshot marketData) {
        return marketData.liquidityScore() < 0.35;
    }

    private boolean hostileVolatility(MarketDataSnapshot marketData) {
        return marketData.volatilityStabilityScore() < 0.35;
    }

    private boolean supportiveRotation(MarketDataSnapshot marketData) {
        return marketData.rangePosition() >= 0.35
                && marketData.rangePosition() <= 0.75;
    }

    private boolean supportiveTrend(MarketDataSnapshot marketData) {
        return marketData.rangePosition() > 0.75
                && marketData.volatilityStabilityScore() >= 0.60;
    }
}
