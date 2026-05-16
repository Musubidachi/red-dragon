package dev.reddragon.domain.models;

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
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.action = Objects.requireNonNull(action, "action is required");
        this.severity = Math.max(0.0, Math.min(1.0, severity));
        this.reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }
}
