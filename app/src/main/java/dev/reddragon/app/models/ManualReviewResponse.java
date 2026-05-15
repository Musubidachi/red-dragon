package dev.reddragon.app.models;

import dev.reddragon.analytics.models.AnalyticsSnapshot;
import dev.reddragon.ingestion.models.TradeCandidate;
import dev.reddragon.marketdata.models.MarketDataSnapshot;
import dev.reddragon.validation.models.ValidationResult;
import lombok.Value;

@Value
public class ManualReviewResponse {
    TradeCandidate candidate;
    MarketDataSnapshot marketData;
    AnalyticsSnapshot analytics;
    ValidationResult validation;

    public ManualReviewResponse(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            AnalyticsSnapshot analytics,
            ValidationResult validation
    ) {
        this.candidate = candidate;
        this.marketData = marketData;
        this.analytics = analytics;
        this.validation = validation;
    }
}
