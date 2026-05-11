package dev.reddragon.analytics.model;

import lombok.Builder;
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

    @Builder
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
        this.regimeCompatibilityScore = clamp(regimeCompatibilityScore);
        this.asymmetryScore = clamp(asymmetryScore);
        this.equilibriumQualityScore = clamp(equilibriumQualityScore);
        this.reflexivityPotentialScore = clamp(reflexivityPotentialScore);
        this.deploymentConfidenceScore = clamp(deploymentConfidenceScore);
        this.reasonNotes = List.copyOf(reasonNotes == null ? List.of() : reasonNotes);
    }

    private static double clamp(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
