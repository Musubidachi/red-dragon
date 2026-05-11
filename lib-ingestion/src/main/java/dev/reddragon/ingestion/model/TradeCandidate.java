package dev.reddragon.ingestion.model;

import java.time.Instant;
import java.util.Objects;

public record TradeCandidate(
        String candidateId,
        String symbol,
        String companyName,
        CandidateCatalystType catalystType,
        SourceType sourceType,
        String sourceId,
        String sourceUrl,
        Instant observedAt,
        String headline,
        String summary,
        double structuralRealityScore,
        double materialSignificanceScore,
        double earlynessScore,
        double reflexivityPotentialScore
) {
    public TradeCandidate {
        candidateId = requireText(candidateId, "candidateId");
        symbol = requireText(symbol, "symbol").toUpperCase();
        companyName = clean(companyName);
        Objects.requireNonNull(catalystType, "catalystType is required");
        Objects.requireNonNull(sourceType, "sourceType is required");
        sourceId = clean(sourceId);
        sourceUrl = clean(sourceUrl);
        observedAt = observedAt == null ? Instant.now() : observedAt;
        headline = clean(headline);
        summary = clean(summary);
        structuralRealityScore = requireNormalized("structuralRealityScore", structuralRealityScore);
        materialSignificanceScore = requireNormalized("materialSignificanceScore", materialSignificanceScore);
        earlynessScore = requireNormalized("earlynessScore", earlynessScore);
        reflexivityPotentialScore = requireNormalized("reflexivityPotentialScore", reflexivityPotentialScore);
    }

    public boolean hasCredibleStructuralCatalyst() {
        return structuralRealityScore >= 0.65;
    }

    private static String requireText(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static double requireNormalized(String fieldName, double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }
}
