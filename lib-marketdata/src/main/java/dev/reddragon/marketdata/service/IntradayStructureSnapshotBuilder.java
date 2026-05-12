package dev.reddragon.marketdata.service;

import dev.reddragon.marketdata.model.IntradayBar;
import dev.reddragon.marketdata.model.MarketIntradayStructureSnapshot;
import dev.reddragon.marketdata.util.MarketMathUtils;

import java.util.List;
import java.util.Objects;

/**
 * Builds derived intraday structure snapshots from normalized bars.
 */
public class IntradayStructureSnapshotBuilder {

    private final VwapCalculator vwapCalculator = new VwapCalculator();
    private final DirectionalPersistenceCalculator persistenceCalculator = new DirectionalPersistenceCalculator();

    /**
     * Main processing flow.
     */
    public MarketIntradayStructureSnapshot process(List<IntradayBar> bars) {
        Objects.requireNonNull(bars, "bars are required");

        if (bars.isEmpty()) {
            throw new IllegalArgumentException("At least one intraday bar is required");
        }

        IntradayBar latest = latestBar(bars);
        double sessionVwap = vwapCalculator.process(bars);
        double vwapDistancePercent = vwapDistancePercent(latest.close(), sessionVwap);
        double directionalPersistence = persistenceCalculator.process(bars);
        double rotationalQuality = 1.0 - directionalPersistence;
        double trendStrength = trendStrength(bars);
        double reclaimStrength = reclaimStrength(latest, sessionVwap);

        return new MarketIntradayStructureSnapshot(
                latest.symbol(),
                sessionVwap,
                latest.close(),
                vwapDistancePercent,
                reclaimStrength,
                directionalPersistence,
                rotationalQuality,
                trendStrength,
                latest.close() >= sessionVwap
        );
    }

    private IntradayBar latestBar(List<IntradayBar> bars) {
        return bars.get(bars.size() - 1);
    }

    private double vwapDistancePercent(double latestClose, double sessionVwap) {
        if (sessionVwap <= 0.0) {
            return 0.0;
        }
        return (latestClose - sessionVwap) / sessionVwap;
    }

    private double reclaimStrength(IntradayBar latest, double sessionVwap) {
        if (latest.range() <= 0.0 || sessionVwap <= 0.0) {
            return 0.50;
        }

        if (latest.close() < sessionVwap) {
            return 0.25;
        }

        double positionAboveVwap = (latest.close() - sessionVwap) / latest.range();
        return MarketMathUtils.clamp(positionAboveVwap);
    }

    private double trendStrength(List<IntradayBar> bars) {
        IntradayBar first = bars.get(0);
        IntradayBar latest = latestBar(bars);

        if (first.close() <= 0.0) {
            return 0.0;
        }

        return MarketMathUtils.clamp(Math.abs((latest.close() - first.close()) / first.close()) * 10.0);
    }
}
