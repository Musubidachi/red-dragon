package dev.reddragon.analytics.models;

import dev.reddragon.analytics.utilities.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

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
        this.regimeLabel = regimeLabel == null ? RegimeLabel.MIXED : regimeLabel;
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
     */
    public String dominantScore() {
        double maxScore = -1;
        String dominant = "regimeCompatibility";
        if (regimeCompatibilityScore > maxScore) { maxScore = regimeCompatibilityScore; dominant = "regimeCompatibility"; }
        if (asymmetryScore > maxScore)            { maxScore = asymmetryScore;            dominant = "asymmetry"; }
        if (equilibriumQualityScore > maxScore)   { maxScore = equilibriumQualityScore;   dominant = "equilibriumQuality"; }
        if (reflexivityPotentialScore > maxScore) { maxScore = reflexivityPotentialScore; dominant = "reflexivityPotential"; }
        if (deploymentConfidenceScore > maxScore) {                                        dominant = "deploymentConfidence"; }
        return dominant;
    }
}
