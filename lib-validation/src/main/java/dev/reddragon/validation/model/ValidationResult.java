package dev.reddragon.validation.model;

import dev.reddragon.validation.util.ValidationScoreUtils;
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
        this.score = ValidationScoreUtils.requireNormalized("score", score);
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

    /**
     * Returns {@code true} if the deployment tier is actionable (at least PROBE).
     * Provides a quick filter for the review surface without inspecting tier directly.
     */
    public boolean isActionable() {
        return deploymentTier != null && deploymentTier.isActionable();
    }

    /**
     * Returns the first reason code's display name, or a fallback string if
     * no reason codes are present.  Useful for one-line summary logging.
     */
    public String primaryReason() {
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            return "No reason codes";
        }
        return reasonCodes.get(0).displayName();
    }
}
