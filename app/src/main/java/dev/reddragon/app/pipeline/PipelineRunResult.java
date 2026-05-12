package dev.reddragon.app.pipeline;

import dev.reddragon.analytics.model.AnalyticsSnapshot;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import dev.reddragon.validation.model.ValidationResult;

/**
 * End-to-end output for one candidate flowing through enrichment, analytics, and validation.
 */
public record PipelineRunResult(
        TradeCandidate candidate,
        MarketDataSnapshot marketData,
        AnalyticsSnapshot analytics,
        ValidationResult validation
) {
}
