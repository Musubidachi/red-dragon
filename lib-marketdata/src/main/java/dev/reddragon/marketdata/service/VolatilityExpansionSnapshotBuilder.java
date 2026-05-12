package dev.reddragon.marketdata.service;

import dev.reddragon.marketdata.model.IntradayBar;
import dev.reddragon.marketdata.model.MarketVolatilityExpansionSnapshot;
import dev.reddragon.marketdata.util.MarketMathUtils;

import java.util.List;
import java.util.Objects;

/**
 * Builds derived volatility expansion snapshots from historical bars.
 */
public class VolatilityExpansionSnapshotBuilder {

    private final AverageTrueRangeCalculator atrCalculator = new AverageTrueRangeCalculator();
    private final RealizedVolatilityCalculator realizedVolatilityCalculator = new RealizedVolatilityCalculator();

    /**
     * Main processing flow.
     */
    public MarketVolatilityExpansionSnapshot process(
            String symbol,
            List<IntradayBar> currentBars,
            List<IntradayBar> baselineBars
    ) {
        Objects.requireNonNull(symbol, "symbol is required");
        Objects.requireNonNull(currentBars, "currentBars are required");
        Objects.requireNonNull(baselineBars, "baselineBars are required");

        double currentAtr = atrCalculator.process(currentBars);
        double baselineAtr = atrCalculator.process(baselineBars);
        double realizedVolatility = realizedVolatilityCalculator.process(currentBars);

        double expansionScore = expansionScore(currentAtr, baselineAtr);
        double compressionScore = 1.0 - expansionScore;

        return new MarketVolatilityExpansionSnapshot(
                symbol,
                currentAtr,
                baselineAtr,
                realizedVolatility,
                expansionScore,
                MarketMathUtils.clamp(compressionScore)
        );
    }

    private double expansionScore(
            double currentAtr,
            double baselineAtr
    ) {
        if (baselineAtr <= 0.0) {
            return 0.50;
        }

        double ratio = currentAtr / baselineAtr;

        if (ratio <= 1.1) {
            return 0.20;
        }

        if (ratio <= 1.5) {
            return 0.50;
        }

        if (ratio <= 2.0) {
            return 0.75;
        }

        return 1.0;
    }
}
