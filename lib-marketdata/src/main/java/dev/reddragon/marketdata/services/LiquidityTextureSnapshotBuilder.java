package dev.reddragon.marketdata.services;

import dev.reddragon.marketdata.models.MarketLiquidityTextureSnapshot;
import dev.reddragon.marketdata.models.OrderBookSnapshot;
import dev.reddragon.marketdata.utilities.MarketMathUtils;

import java.util.List;
import java.util.Objects;

/**
 * Builds derived liquidity texture snapshots from order-book history.
 */
public class LiquidityTextureSnapshotBuilder {

    private final SpreadQualityCalculator spreadQualityCalculator = new SpreadQualityCalculator();
    private final LiquidityConsistencyCalculator liquidityConsistencyCalculator = new LiquidityConsistencyCalculator();

    /**
     * Main processing flow.
     */
    public MarketLiquidityTextureSnapshot process(
            List<OrderBookSnapshot> snapshots,
            double relativeVolumeScore
    ) {
        Objects.requireNonNull(snapshots, "snapshots are required");

        if (snapshots.isEmpty()) {
            throw new IllegalArgumentException("At least one order-book snapshot is required");
        }

        OrderBookSnapshot latest = snapshots.get(snapshots.size() - 1);

        double spreadQuality = spreadQualityCalculator.process(latest);
        double liquidityConsistency = liquidityConsistencyCalculator.process(snapshots);
        double depthScore = depthScore(latest);
        double slippageRisk = slippageRisk(latest, liquidityConsistency);

        return new MarketLiquidityTextureSnapshot(
                latest.symbol(),
                spreadQuality,
                depthScore,
                liquidityConsistency,
                slippageRisk,
                MarketMathUtils.clamp(relativeVolumeScore)
        );
    }

    private double depthScore(OrderBookSnapshot latest) {
        long totalDepth = latest.bidSize() + latest.askSize();

        if (totalDepth >= 1_000_000L) {
            return 1.0;
        }

        if (totalDepth >= 250_000L) {
            return 0.80;
        }

        if (totalDepth >= 50_000L) {
            return 0.55;
        }

        return 0.25;
    }

    private double slippageRisk(
            OrderBookSnapshot latest,
            double liquidityConsistency
    ) {
        double spreadPenalty = latest.spreadPercent() * 50.0;
        double consistencyPenalty = 1.0 - liquidityConsistency;

        return MarketMathUtils.clamp(
                spreadPenalty * 0.60
                        + consistencyPenalty * 0.40
        );
    }
}
