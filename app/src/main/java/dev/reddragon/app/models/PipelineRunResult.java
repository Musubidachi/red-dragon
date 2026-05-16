package dev.reddragon.app.models;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.ValidationResult;

/**
 * End-to-end output for one candidate flowing through enrichment, analytics, and validation.
 *
 * <p>When {@code duplicate} is {@code true} the candidate was already present in the
 * database and the pipeline was skipped; only {@code candidate} is populated.
 */
public record PipelineRunResult(
        TradeCandidate candidate,
        MarketDataSnapshot marketData,
        AnalyticsSnapshot analytics,
        ValidationResult validation,
        boolean duplicate
) {
    /** Normal result from a full pipeline run. */
    public static PipelineRunResult of(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            AnalyticsSnapshot analytics,
            ValidationResult validation
    ) {
        return new PipelineRunResult(candidate, marketData, analytics, validation, false);
    }

    /** Returned when the candidate already exists in the database and was skipped. */
    public static PipelineRunResult duplicate(TradeCandidate candidate) {
        return new PipelineRunResult(candidate, null, null, null, true);
    }

    /**
     * One-line summary string for logging or compact display.
     * Format: {@code [SYMBOL] PASS (0.83) → STANDARD | duplicate=false}
     */
    public String verdictSummary() {
        if (duplicate) {
            return "[" + candidate.symbol() + "] DUPLICATE — skipped";
        }
        if (validation == null) {
            return "[" + candidate.symbol() + "] no validation result";
        }
        return String.format("[%s] %s (%.2f) → %s | duplicate=false",
                candidate.symbol(),
                validation.verdict().displayName(),
                validation.score(),
                validation.deploymentTier().displayName());
    }
}
