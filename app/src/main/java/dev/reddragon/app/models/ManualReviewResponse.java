package dev.reddragon.app.models;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.ValidationResult;
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
