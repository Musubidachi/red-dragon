package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;

import java.util.List;
import java.util.Objects;

/**
 * Calculates average true range from intraday bars.
 */
public class AverageTrueRangeCalculator {

    /**
     * Main processing flow.
     */
    public double process(List<IntradayBar> bars) {
        Objects.requireNonNull(bars, "bars are required");

        if (bars.size() < 2) {
            return 0.0;
        }

        double total = 0.0;

        for (int index = 1; index < bars.size(); index++) {
            IntradayBar current = bars.get(index);
            IntradayBar previous = bars.get(index - 1);

            total += trueRange(current, previous);
        }

        return total / (bars.size() - 1);
    }

    private double trueRange(
            IntradayBar current,
            IntradayBar previous
    ) {
        double highLow = current.high() - current.low();
        double highClose = Math.abs(current.high() - previous.close());
        double lowClose = Math.abs(current.low() - previous.close());

        return Math.max(highLow, Math.max(highClose, lowClose));
    }
}
