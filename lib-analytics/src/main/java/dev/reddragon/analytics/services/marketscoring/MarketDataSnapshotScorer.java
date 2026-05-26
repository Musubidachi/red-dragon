package dev.reddragon.analytics.services.marketscoring;

import java.util.Objects;

import dev.reddragon.domain.models.MarketDataSnapshot;

/**
 * Enrichment step that turns a raw {@link MarketDataSnapshot} (with
 * {@code liquidityScore} and {@code volatilityStabilityScore} set to
 * placeholder zeros by {@code MarketFeatureCalculator}) into a scored
 * snapshot.
 *
 * <p>Composes the two underlying scorers; intended to be applied
 * immediately after the marketdata step so every downstream consumer
 * (analytics, validation, persistence) sees the same enriched values.
 * See lib-marketdata REVIEW.md Finding #8.
 */
public class MarketDataSnapshotScorer {

    private final LiquidityScorer liquidityScorer;
    private final VolatilityStabilityScorer volatilityStabilityScorer;

    public MarketDataSnapshotScorer() {
        this(new LiquidityScorer(), new VolatilityStabilityScorer());
    }

    public MarketDataSnapshotScorer(
            LiquidityScorer liquidityScorer,
            VolatilityStabilityScorer volatilityStabilityScorer
    ) {
        this.liquidityScorer = Objects.requireNonNull(liquidityScorer, "liquidityScorer is required");
        this.volatilityStabilityScorer = Objects.requireNonNull(volatilityStabilityScorer, "volatilityStabilityScorer is required");
    }

    /**
     * Return a copy of {@code snapshot} with the two score fields filled
     * in. Null-tolerant — returns {@code null} when the input is null so
     * callers in error-handling paths don't need to add a guard.
     */
    public MarketDataSnapshot process(MarketDataSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        double liquidity = liquidityScorer.process(snapshot.averageVolume());
        double volatility = volatilityStabilityScorer.process(
                snapshot.latestClose(), snapshot.averageTrueRange());
        return snapshot.withScores(liquidity, volatility);
    }
}
