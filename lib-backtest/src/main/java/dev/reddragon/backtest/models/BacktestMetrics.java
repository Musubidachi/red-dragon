package dev.reddragon.backtest.models;

import dev.reddragon.domain.models.Verdict;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record BacktestMetrics(
        int totalFrames,
        double averageScore,
        Map<Verdict, Long> verdictCounts
) {
    /**
     * Compact constructor wraps {@code verdictCounts} in an unmodifiable view
     * so a downstream caller cannot mutate the map and break determinism of
     * the enclosing {@link BacktestReport}.
     */
    public BacktestMetrics {
        verdictCounts = verdictCounts == null
                ? Map.of()
                : Collections.unmodifiableMap(new EnumMap<>(verdictCounts));
    }

    public static BacktestMetrics from(List<BacktestOutcome> outcomes) {
        List<BacktestOutcome> safeOutcomes = outcomes == null ? List.of() : outcomes;
        double averageScore = safeOutcomes.stream()
                .mapToDouble(outcome -> outcome.validation().score())
                .average()
                .orElse(0.0);

        Map<Verdict, Long> counts = new EnumMap<>(Verdict.class);
        for (BacktestOutcome outcome : safeOutcomes) {
            Verdict verdict = outcome.validation().verdict();
            counts.put(verdict, counts.getOrDefault(verdict, 0L) + 1L);
        }

        return new BacktestMetrics(safeOutcomes.size(), averageScore, counts);
    }

    /**
     * Fraction of frames that resulted in a PASS verdict (0.0 if no frames).
     */
    public double passRate() {
        if (totalFrames == 0) return 0.0;
        return verdictCounts.getOrDefault(Verdict.PASS, 0L) / (double) totalFrames;
    }

    /**
     * Fraction of frames that resulted in a WATCH verdict (0.0 if no frames).
     */
    public double watchRate() {
        if (totalFrames == 0) return 0.0;
        return verdictCounts.getOrDefault(Verdict.WATCH, 0L) / (double) totalFrames;
    }

    /**
     * Fraction of frames that resulted in a REJECT verdict (0.0 if no frames).
     */
    public double rejectRate() {
        if (totalFrames == 0) return 0.0;
        return verdictCounts.getOrDefault(Verdict.REJECT, 0L) / (double) totalFrames;
    }
}
