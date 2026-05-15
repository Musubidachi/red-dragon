package dev.reddragon.validation.models;

import dev.reddragon.validation.models.DeploymentTier;
import dev.reddragon.validation.models.RiskFlag;
import dev.reddragon.validation.models.Verdict;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * Compact human-readable summary of a validation review.
 */
@Value
@Accessors(fluent = true)
public class ValidationSummary {
    String candidateId;
    String symbol;
    Verdict verdict;
    DeploymentTier deploymentTier;
    double score;
    double validationConfidenceScore;
    List<RiskFlag> riskFlags;
    String headline;
    String summaryText;

    public ValidationSummary(
            String candidateId,
            String symbol,
            Verdict verdict,
            DeploymentTier deploymentTier,
            double score,
            double validationConfidenceScore,
            List<RiskFlag> riskFlags,
            String headline,
            String summaryText
    ) {
        this.candidateId = Objects.requireNonNull(candidateId, "candidateId is required");
        this.symbol = Objects.requireNonNull(symbol, "symbol is required");
        this.verdict = Objects.requireNonNull(verdict, "verdict is required");
        this.deploymentTier = Objects.requireNonNull(deploymentTier, "deploymentTier is required");
        this.score = score;
        this.validationConfidenceScore = validationConfidenceScore;
        this.riskFlags = List.copyOf(riskFlags == null ? List.of() : riskFlags);
        this.headline = headline == null ? "" : headline;
        this.summaryText = summaryText == null ? "" : summaryText;
    }
}
