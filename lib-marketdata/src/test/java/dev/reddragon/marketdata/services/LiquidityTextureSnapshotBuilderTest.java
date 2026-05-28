package dev.reddragon.marketdata.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.reddragon.domain.models.MarketLiquidityTextureSnapshot;
import dev.reddragon.domain.models.OrderBookSnapshot;

class LiquidityTextureSnapshotBuilderTest {

    private final LiquidityTextureSnapshotBuilder builder = new LiquidityTextureSnapshotBuilder();

    @Test
    void nullSnapshotsRejected() {
        assertThrows(NullPointerException.class, () -> builder.process(null, 0.5));
    }

    @Test
    void emptySnapshotsRejected() {
        assertThrows(IllegalArgumentException.class, () -> builder.process(List.of(), 0.5));
    }

    @Test
    void latestSnapshotDrivesSymbolSpreadDepthAndSlippage() {
        List<OrderBookSnapshot> snapshots = List.of(
                book("MSFT", 100.00, 100.01, 20_000L, 20_000L, 0.0001, 0.50),
                book(" msft ", 99.00, 109.00, 600_000L, 500_000L, 0.1000, 0.00)
        );

        MarketLiquidityTextureSnapshot snapshot = builder.process(snapshots, 1.8);

        assertEquals("MSFT", snapshot.symbol());
        assertEquals(0.15, snapshot.spreadQualityScore(), 1e-9);
        assertEquals(1.0, snapshot.orderBookDepthScore(), 1e-9);
        assertEquals(1.0, snapshot.slippageRiskScore(), 1e-9);
        assertEquals(1.0, snapshot.relativeVolumeScore(), 1e-9);
    }

    @Test
    void thinDepthAndNegativeRelativeVolumeAreClamped() {
        MarketLiquidityTextureSnapshot snapshot = builder.process(
                List.of(book("AAPL", 10.00, 10.05, 1_000L, 2_000L, 0.005, 0.50)),
                -0.25
        );

        assertEquals(0.25, snapshot.orderBookDepthScore(), 1e-9);
        assertEquals(0.0, snapshot.relativeVolumeScore(), 1e-9);
    }

    private static OrderBookSnapshot book(
            String symbol,
            double bid,
            double ask,
            long bidSize,
            long askSize,
            double spreadPercent,
            double imbalanceScore
    ) {
        return new OrderBookSnapshot(
                symbol,
                bid,
                ask,
                bidSize,
                askSize,
                spreadPercent,
                imbalanceScore
        );
    }
}
