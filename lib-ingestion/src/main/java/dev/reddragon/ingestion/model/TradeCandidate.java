package dev.reddragon.ingestion.model;

import dev.reddragon.ingestion.util.IngestionTextUtils;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Objects;

@Value
@Accessors(fluent = true)
public class TradeCandidate {
    String candidateId;
    String symbol;
    String companyName;
    CandidateCatalystType catalystType;
    SourceType sourceType;
    String sourceId;
    String sourceUrl;
    Instant observedAt;
    String headline;
    String summary;
    double structuralRealityScore;
    double materialSignificanceScore;
    double earlynessScore;
    double reflexivityPotentialScore;

    public TradeCandidate(
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
        this.candidateId = IngestionTextUtils.requireText(candidateId, "candidateId");
        this.symbol = IngestionTextUtils.normalizeSymbol(symbol);
        this.companyName = IngestionTextUtils.clean(companyName);
        this.catalystType = Objects.requireNonNull(catalystType, "catalystType is required");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType is required");
        this.sourceId = IngestionTextUtils.clean(sourceId);
        this.sourceUrl = IngestionTextUtils.clean(sourceUrl);
        this.observedAt = observedAt == null ? Instant.now() : observedAt;
        this.headline = IngestionTextUtils.clean(headline);
        this.summary = IngestionTextUtils.clean(summary);
        this.structuralRealityScore = requireNormalized("structuralRealityScore", structuralRealityScore);
        this.materialSignificanceScore = requireNormalized("materialSignificanceScore", materialSignificanceScore);
        this.earlynessScore = requireNormalized("earlynessScore", earlynessScore);
        this.reflexivityPotentialScore = requireNormalized("reflexivityPotentialScore", reflexivityPotentialScore);
    }

    public boolean hasCredibleStructuralCatalyst() {
        return structuralRealityScore >= 0.65;
    }

    private static double requireNormalized(String fieldName, double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }
}
