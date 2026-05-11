package dev.reddragon.app.api;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import dev.reddragon.validation.model.ValidationResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualReviewResponse {
    TradeCandidate candidate;
    MarketDataSnapshot marketData;
    AnalyticsSnapshot analytics;
    ValidationResult validation;
}
