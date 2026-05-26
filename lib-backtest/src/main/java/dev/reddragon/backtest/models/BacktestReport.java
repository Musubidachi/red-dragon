package dev.reddragon.backtest.models;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record BacktestReport(
        String strategyName,
        BacktestMetrics metrics,
        List<BacktestOutcome> outcomes
) {
    public BacktestReport {
        Objects.requireNonNull(strategyName, "strategyName is required");
        Objects.requireNonNull(metrics, "metrics is required");
        outcomes = List.copyOf(outcomes == null ? List.of() : outcomes);
    }

    /**
     * Convenience delegate to {@link BacktestMetrics#passRate()}.
     * Returns the fraction of frames that produced a PASS verdict.
     */
    public double passRate() {
        return metrics.passRate();
    }

    /**
     * One-line narrative summary of the backtest run, suitable for logging or
     * compact display in review surfaces.
     *
     * <p>Example: {@code "DisequilibriumV1: 120 frames | pass=42% watch=28% reject=30% | avg score=0.71"}
     *
     * <p>Locale is pinned to {@link Locale#ROOT} so the decimal separator stays
     * {@code "."} regardless of the JVM's default locale — without this, a
     * comma-decimal deployment locale would produce {@code "avg score=0,71"}.
     */
    public String summary() {
        return String.format(Locale.ROOT,
                "%s: %d frames | pass=%.0f%% watch=%.0f%% reject=%.0f%% | avg score=%.2f",
                strategyName,
                metrics.totalFrames(),
                metrics.passRate() * 100,
                metrics.watchRate() * 100,
                metrics.rejectRate() * 100,
                metrics.averageScore());
    }
}
