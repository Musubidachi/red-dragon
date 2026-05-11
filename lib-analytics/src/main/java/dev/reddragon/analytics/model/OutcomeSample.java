package dev.reddragon.analytics.model;

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
    double daysHeld;
    boolean thesisWorked;

    public OutcomeSample(
            String candidateId,
            String symbol,
            Instant observedAt,
            AnalyticsScoreBreakdown scoreBreakdown,
            double realizedReturn,
            double maxDrawdown,
            double daysHeld,
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
}
