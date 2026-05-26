package dev.reddragon.analytics.services.marketscoring;

import dev.reddragon.analytics.config.MarketScoringProperties;

/**
 * Bucketed 0..1 score for a symbol's average daily volume.
 *
 * <p>Lives in {@code lib-analytics} (L4) — see lib-marketdata REVIEW.md
 * Finding #8 for the architectural rationale. {@code lib-marketdata}
 * emits only raw features ({@code averageVolume}); this scorer applies
 * the policy that turns a raw feature into a normalised score.
 */
public class LiquidityScorer {

    private final MarketScoringProperties properties;

    public LiquidityScorer() {
        this(MarketScoringProperties.defaults());
    }

    public LiquidityScorer(MarketScoringProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties is required");
        }
        this.properties = properties;
    }

    /**
     * @param averageVolume non-negative average daily volume.
     * @return a normalised score in [0.0, 1.0] reflecting the volume
     *         bucket the symbol falls into.
     */
    public double process(double averageVolume) {
        if (averageVolume >= properties.getLiquidityVolumeHigh())
            return properties.getLiquidityScoreHigh();
        if (averageVolume >= properties.getLiquidityVolumeMediumHigh())
            return properties.getLiquidityScoreMediumHigh();
        if (averageVolume >= properties.getLiquidityVolumeMedium())
            return properties.getLiquidityScoreMedium();
        if (averageVolume >= properties.getLiquidityVolumeLow())
            return properties.getLiquidityScoreLow();
        return properties.getLiquidityScoreThin();
    }
}
