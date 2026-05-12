package dev.reddragon.backtest.model;

import java.util.List;

public record BacktestReport(
        String strategyName,
        BacktestMetrics metrics,
        List<BacktestOutcome> outcomes
) {
    public BacktestReport {
        outcomes = List.copyOf(outcomes == null ? List.of() : outcomes);
    }
}
