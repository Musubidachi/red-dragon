package dev.reddragon.backtest.model;

import dev.reddragon.validation.model.Verdict;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record BacktestMetrics(
        int totalFrames,
        double averageScore,
        Map<Verdict, Long> verdictCounts
) {
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
}
