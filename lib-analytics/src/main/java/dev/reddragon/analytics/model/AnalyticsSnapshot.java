package dev.reddragon.analytics.model;

import java.time.Instant;
import java.util.List;

public record AnalyticsSnapshot(
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
    public AnalyticsSnapshot {
        if (candidateId == null || candidateId.isBlank()) {
            throw new IllegalArgumentException("candidateId is required");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        symbol = symbol.trim().toUpperCase();
        observedAt = observedAt == null ? Instant.now() : observedAt;
        regimeLabel = regimeLabel == null ? RegimeLabel.MIXED : regimeLabel;
        regimeCompatibilityScore = clamp(regimeCompatibilityScore);
        asymmetryScore = clamp(asymmetryScore);
        equilibriumQualityScore = clamp(equilibriumQualityScore);
        reflexivityPotentialScore = clamp(reflexivityPotentialScore);
        deploymentConfidenceScore = clamp(deploymentConfidenceScore);
        reasonNotes = List.copyOf(reasonNotes == null ? List.of() : reasonNotes);
    }

    private static double clamp(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
