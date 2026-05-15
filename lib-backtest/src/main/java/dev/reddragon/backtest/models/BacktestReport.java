package dev.reddragon.backtest.models;

import java.util.List;

public record BacktestReport(
        String strategyName,
        BacktestMetrics metrics,
        List<BacktestOutcome> outcomes
) {
    public BacktestReport {
        outcomes = List.copyOf(outcomes == null ? List.of() : outcomes);
    }

    /**
     * Convenience delegate to {@link BacktestMetrics#passRate()}.
     * Returns the fraction of frames that produced a PASS verdict.
     */
    public double passRate() {
        return metrics == null ? 0.0 : metrics.passRate();
    }

    /**
     * One-line narrative summary of the backtest run, suitable for logging or
     * compact display in review surfaces.
     *
     * <p>Example: {@code "DisequilibriumV1: 120 frames | pass=42% watch=28% reject=30% | avg score=0.71"}
     */
    public String summary() {
        if (metrics == null) {
            return strategyName + ": no metrics";
        }
        return String.format("%s: %d frames | pass=%.0f%% watch=%.0f%% reject=%.0f%% | avg score=%.2f",
                strategyName,
                metrics.totalFrames(),
                metrics.passRate() * 100,
                metrics.watchRate() * 100,
                metrics.rejectRate() * 100,
                metrics.averageScore());
    }
}
