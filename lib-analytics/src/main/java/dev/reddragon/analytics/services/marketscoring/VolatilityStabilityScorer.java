package dev.reddragon.analytics.services.marketscoring;

import dev.reddragon.analytics.config.MarketScoringProperties;

/**
 * Bucketed 0..1 score expressing how stable (low-volatility) the symbol's
 * recent ATR profile is, relative to its last close.
 *
 * <p>Lives in {@code lib-analytics} (L4) — see lib-marketdata REVIEW.md
 * Finding #8 for the architectural rationale.
 */
public class VolatilityStabilityScorer {

    private final MarketScoringProperties properties;

    public VolatilityStabilityScorer() {
        this(MarketScoringProperties.defaults());
    }

    public VolatilityStabilityScorer(MarketScoringProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties is required");
        }
        this.properties = properties;
    }

    /**
     * @param latestClose       most recent close — must be positive to
     *                          produce a meaningful ATR%.
     * @param averageTrueRange  non-negative ATR over the lookback window.
     * @return a normalised score in [0.0, 1.0]. Returns
     *         {@code stabilityUnknownSentinel} (0.50 by default) when
     *         either input is non-positive, since the percentage is
     *         undefined.
     */
    public double process(double latestClose, double averageTrueRange) {
        if (latestClose <= 0 || averageTrueRange <= 0) {
            return properties.getStabilityUnknownSentinel();
        }
        double atrPercent = averageTrueRange / latestClose;
        if (atrPercent <= properties.getAtrPercentVeryLow())
            return properties.getStabilityScoreVeryHigh();
        if (atrPercent <= properties.getAtrPercentLow())
            return properties.getStabilityScoreHigh();
        if (atrPercent <= properties.getAtrPercentMedium())
            return properties.getStabilityScoreMedium();
        if (atrPercent <= properties.getAtrPercentHigh())
            return properties.getStabilityScoreLow();
        return properties.getStabilityScoreVeryLow();
    }
}
