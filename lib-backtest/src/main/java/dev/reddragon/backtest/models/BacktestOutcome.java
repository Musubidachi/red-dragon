package dev.reddragon.backtest.models;

import dev.reddragon.analytics.models.AnalyticsSnapshot;
import dev.reddragon.ingestion.models.TradeCandidate;
import dev.reddragon.marketdata.models.MarketDataSnapshot;
import dev.reddragon.validation.models.ValidationResult;

public record BacktestOutcome(
        TradeCandidate candidate,
        MarketDataSnapshot marketData,
        AnalyticsSnapshot analytics,
        ValidationResult validation
) {
}
