package dev.reddragon.backtest.models;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.ValidationResult;

import java.util.Objects;

public record BacktestOutcome(
        TradeCandidate candidate,
        MarketDataSnapshot marketData,
        AnalyticsSnapshot analytics,
        ValidationResult validation
) {
    public BacktestOutcome {
        Objects.requireNonNull(candidate, "candidate is required");
        Objects.requireNonNull(marketData, "marketData is required");
        Objects.requireNonNull(analytics, "analytics is required");
        Objects.requireNonNull(validation, "validation is required");
    }
}
