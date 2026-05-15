package dev.reddragon.validation.models;

import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a completed validation review.
 */
@Value
@Accessors(fluent = true)
public class ValidationAudit {
    String candidateId;
    String symbol;
    Instant validatedAt;
    ValidationResult validationResult;
    List<RiskFlag> riskFlags;
    double validationConfidenceScore;

    public ValidationAudit(
            String candidateId,
            String symbol,
            Instant validatedAt,
            ValidationResult validationResult,
            List<RiskFlag> riskFlags,
            double validationConfidenceScore
    ) {
        this.candidateId = Objects.requireNonNull(candidateId, "candidateId is required");
        this.symbol = Objects.requireNonNull(symbol, "symbol is required");
        this.validatedAt = Objects.requireNonNull(validatedAt, "validatedAt is required");
        this.validationResult = Objects.requireNonNull(validationResult, "validationResult is required");
        this.riskFlags = List.copyOf(riskFlags == null ? List.of() : riskFlags);
        this.validationConfidenceScore = validationConfidenceScore;
    }
}
