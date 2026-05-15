package dev.reddragon.marketdata.services;

import dev.reddragon.marketdata.models.IntradayBar;

import java.util.List;
import java.util.Objects;

/**
 * Calculates relative volume compared to historical baseline volume.
 */
public class RelativeVolumeCalculator {

    /**
     * Main processing flow.
     */
    public double process(
            List<IntradayBar> currentBars,
            List<IntradayBar> baselineBars
    ) {
        Objects.requireNonNull(currentBars, "currentBars are required");
        Objects.requireNonNull(baselineBars, "baselineBars are required");

        long currentVolume = totalVolume(currentBars);
        long baselineVolume = totalVolume(baselineBars);

        if (baselineVolume == 0L) {
            return 0.0;
        }

        return (double) currentVolume / baselineVolume;
    }

    private long totalVolume(List<IntradayBar> bars) {
        long total = 0L;

        for (IntradayBar bar : bars) {
            total += bar.volume();
        }

        return total;
    }
}
