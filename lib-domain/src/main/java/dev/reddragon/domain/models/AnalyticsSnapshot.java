package dev.reddragon.domain.models;

import dev.reddragon.math.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Value
@Accessors(fluent = true)
public class AnalyticsSnapshot {
    String candidateId;
    String symbol;
    Instant observedAt;
    RegimeLabel regimeLabel;
    double regimeCompatibilityScore;
    double asymmetryScore;
    double equilibriumQualityScore;
    double reflexivityPotentialScore;
    double deploymentConfidenceScore;
    List<String> reasonNotes;

    public AnalyticsSnapshot(
            String candidateId,
            String symbol,
            Instant observedAt,
            RegimeLabel regimeLabel,
            double regimeCompatibilityScore,
            double asymmetryScore,
            double equilibriumQualityScore,
            double reflexivityPotentialScore,
            double deploymentConfidenceScore,
            List<String> reasonNotes
    ) {
        if (candidateId == null || candidateId.isBlank()) {
            throw new IllegalArgumentException("candidateId is required");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        this.candidateId = candidateId.trim();
        this.symbol = symbol.trim().toUpperCase();
        this.observedAt = observedAt == null ? Instant.now() : observedAt;
        this.regimeLabel = Objects.requireNonNull(regimeLabel, "regimeLabel is required");
        this.regimeCompatibilityScore = AnalyticsScoreUtils.clamp(regimeCompatibilityScore);
        this.asymmetryScore = AnalyticsScoreUtils.clamp(asymmetryScore);
        this.equilibriumQualityScore = AnalyticsScoreUtils.clamp(equilibriumQualityScore);
        this.reflexivityPotentialScore = AnalyticsScoreUtils.clamp(reflexivityPotentialScore);
        this.deploymentConfidenceScore = AnalyticsScoreUtils.clamp(deploymentConfidenceScore);
        this.reasonNotes = List.copyOf(reasonNotes == null ? List.of() : reasonNotes);
    }

    /**
     * Returns the name of the dimension with the highest score, useful for
     * one-line summary logging and review display.
     *
     * <p>Each comparison updates both {@code maxScore} and {@code dominant}
     * consistently so that adding a new dimension at the end of the chain
     * does not require special-casing the previous last line.
     */
    public String dominantScore() {
        double maxScore = regimeCompatibilityScore;
        String dominant = "regimeCompatibility";
        if (asymmetryScore > maxScore)            { maxScore = asymmetryScore;            dominant = "asymmetry"; }
        if (equilibriumQualityScore > maxScore)   { maxScore = equilibriumQualityScore;   dominant = "equilibriumQuality"; }
        if (reflexivityPotentialScore > maxScore) { maxScore = reflexivityPotentialScore; dominant = "reflexivityPotential"; }
        if (deploymentConfidenceScore > maxScore) { maxScore = deploymentConfidenceScore; dominant = "deploymentConfidence"; }
        return dominant;
    }
}
