package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Advisory modification request generated from live analytics changes.
 */
@Value
@Accessors(fluent = true)
public class TradeModificationRequest {
    String candidateId;
    String symbol;
    Instant createdAt;
    TradeModificationAction action;
    double severity;
    List<String> reasons;

    public TradeModificationRequest(
            String candidateId,
            String symbol,
            Instant createdAt,
            TradeModificationAction action,
            double severity,
            List<String> reasons
    ) {
        this.candidateId = Objects.requireNonNull(candidateId, "candidateId is required");
        this.symbol = Objects.requireNonNull(symbol, "symbol is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.action = Objects.requireNonNull(action, "action is required");
        this.severity = DomainScorePolicy.clampDerivedScore(severity);
        this.reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }
}
