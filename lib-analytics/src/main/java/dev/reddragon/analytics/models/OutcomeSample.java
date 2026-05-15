package dev.reddragon.analytics.models;

import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Objects;

/**
 * Historical sample used for long-horizon calibration.
 */
@Value
@Accessors(fluent = true)
public class OutcomeSample {
    String candidateId;
    String symbol;
    Instant observedAt;
    AnalyticsScoreBreakdown scoreBreakdown;
    double realizedReturn;
    double maxDrawdown;
    int daysHeld;
    boolean thesisWorked;

    public OutcomeSample(
            String candidateId,
            String symbol,
            Instant observedAt,
            AnalyticsScoreBreakdown scoreBreakdown,
            double realizedReturn,
            double maxDrawdown,
            int daysHeld,
            boolean thesisWorked
    ) {
        this.candidateId = Objects.requireNonNull(candidateId, "candidateId is required");
        this.symbol = Objects.requireNonNull(symbol, "symbol is required");
        this.observedAt = observedAt == null ? Instant.now() : observedAt;
        this.scoreBreakdown = Objects.requireNonNull(scoreBreakdown, "scoreBreakdown is required");
        this.realizedReturn = realizedReturn;
        this.maxDrawdown = maxDrawdown;
        this.daysHeld = daysHeld;
        this.thesisWorked = thesisWorked;
    }

    /**
     * Classifies the realized return into a named category for bucketed analysis.
     *
     * @return "STRONG_WIN" (&gt;+20%), "WIN" (&gt;+5%), "FLAT" (±5%), "LOSS" (&lt;-5%),
     *         or "LARGE_LOSS" (&lt;-20%)
     */
    public String returnCategory() {
        if (realizedReturn > 0.20)  return "STRONG_WIN";
        if (realizedReturn > 0.05)  return "WIN";
        if (realizedReturn >= -0.05) return "FLAT";
        if (realizedReturn >= -0.20) return "LOSS";
        return "LARGE_LOSS";
    }
}
