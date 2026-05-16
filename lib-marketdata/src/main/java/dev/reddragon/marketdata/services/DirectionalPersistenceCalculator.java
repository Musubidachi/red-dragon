package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;

import java.util.List;
import java.util.Objects;

/**
 * Measures persistence of directional continuation behavior.
 */
public class DirectionalPersistenceCalculator {

    /**
     * Main processing flow.
     */
    public double process(List<IntradayBar> bars) {
        Objects.requireNonNull(bars, "bars are required");

        if (bars.size() < 3) {
            return 0.0;
        }

        int persistenceCount = 0;
        int comparisons = 0;

        for (int index = 2; index < bars.size(); index++) {
            double priorMove = move(
                    bars.get(index - 2),
                    bars.get(index - 1)
            );

            double currentMove = move(
                    bars.get(index - 1),
                    bars.get(index)
            );

            if (sameDirection(priorMove, currentMove)) {
                persistenceCount++;
            }

            comparisons++;
        }

        if (comparisons == 0) {
            return 0.0;
        }

        return (double) persistenceCount / comparisons;
    }

    private double move(
            IntradayBar previous,
            IntradayBar current
    ) {
        return current.close() - previous.close();
    }

    private boolean sameDirection(
            double priorMove,
            double currentMove
    ) {
        return (priorMove > 0.0 && currentMove > 0.0)
                || (priorMove < 0.0 && currentMove < 0.0);
    }
}
