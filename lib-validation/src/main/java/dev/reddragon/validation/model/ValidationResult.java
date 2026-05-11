package dev.reddragon.validation.model;

import java.util.List;
import java.util.Objects;

/**
 * Complete validation output for one candidate.
 */
public record ValidationResult(
        String candidateId,
        String symbol,
        Verdict verdict,
        DeploymentTier deploymentTier,
        double score,
        List<ValidationFactor> factors,
        List<ReasonCode> reasonCodes,
        List<String> explanations
) {
    public ValidationResult {
        Objects.requireNonNull(candidateId, "candidateId is required");
        Objects.requireNonNull(symbol, "symbol is required");
        Objects.requireNonNull(verdict, "verdict is required");
        Objects.requireNonNull(deploymentTier, "deploymentTier is required");
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0");
        }
        factors = List.copyOf(factors == null ? List.of() : factors);
        reasonCodes = List.copyOf(reasonCodes == null ? List.of() : reasonCodes);
        explanations = List.copyOf(explanations == null ? List.of() : explanations);
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
