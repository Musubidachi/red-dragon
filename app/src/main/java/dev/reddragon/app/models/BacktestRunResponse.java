package dev.reddragon.app.models;

import dev.reddragon.backtest.models.BacktestReport;

public record BacktestRunResponse(
        String runId,
        BacktestReport report
) {
}
