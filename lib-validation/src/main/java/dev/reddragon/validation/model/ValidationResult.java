package dev.reddragon.validation.model;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * Complete validation output for one candidate.
 */
@Value
@Accessors(fluent = true)
public class ValidationResult {
    String candidateId;
    String symbol;
    Verdict verdict;
    DeploymentTier deploymentTier;
    double score;
    List<ValidationFactor> factors;
    List<ReasonCode> reasonCodes;
    List<String> explanations;

    @Builder
    public ValidationResult(
            String candidateId,
            String symbol,
            Verdict verdict,
            DeploymentTier deploymentTier,
            double score,
            List<ValidationFactor> factors,
            List<ReasonCode> reasonCodes,
            List<String> explanations
    ) {
        this.candidateId = Objects.requireNonNull(candidateId, "candidateId is required");
        this.symbol = Objects.requireNonNull(symbol, "symbol is required");
        this.verdict = Objects.requireNonNull(verdict, "verdict is required");
        this.deploymentTier = Objects.requireNonNull(deploymentTier, "deploymentTier is required");
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0");
        }
        this.score = score;
        this.factors = List.copyOf(factors == null ? List.of() : factors);
        this.reasonCodes = List.copyOf(reasonCodes == null ? List.of() : reasonCodes);
        this.explanations = List.copyOf(explanations == null ? List.of() : explanations);
    }

    public boolean passed() {
        return verdict == Verdict.PASS;
    }

    public boolean rejected() {
        return verdict == Verdict.REJECT;
    }

    public boolean watch() {
        return verdict == Verdict.WATCH;
    }
}
