package dev.reddragon.backtest.model;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import dev.reddragon.validation.model.ValidationResult;

public record BacktestOutcome(
        TradeCandidate candidate,
        MarketDataSnapshot marketData,
        AnalyticsSnapshot analytics,
        ValidationResult validation
) {
}
