package dev.reddragon.backtest.models;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.ValidationResult;

public record BacktestOutcome(
        TradeCandidate candidate,
        MarketDataSnapshot marketData,
        AnalyticsSnapshot analytics,
        ValidationResult validation
) {
}
