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

    /** Returns a new {@link Builder} pre-populated with this candidate's values. */
    public Builder toBuilder() {
        return new Builder()
                .candidateId(candidateId)
                .symbol(symbol)
                .companyName(companyName)
                .catalystType(catalystType)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .sourceUrl(sourceUrl)
                .observedAt(observedAt)
                .headline(headline)
                .summary(summary)
                .structuralRealityScore(structuralRealityScore)
                .materialSignificanceScore(materialSignificanceScore)
                .earlynessScore(earlynessScore)
                .reflexivityPotentialScore(reflexivityPotentialScore);
    }

    /** Fluent builder for {@link TradeCandidate}. */
    public static final class Builder {
        private String candidateId;
        private String symbol;
        private String companyName;
        private CandidateCatalystType catalystType;
        private SourceType sourceType = SourceType.MANUAL;
        private String sourceId;
        private String sourceUrl;
        private Instant observedAt;
        private String headline;
        private String summary;
        private double structuralRealityScore;
        private double materialSignificanceScore;
        private double earlynessScore;
        private double reflexivityPotentialScore;

        public Builder candidateId(String v)                { this.candidateId = v; return this; }
        public Builder symbol(String v)                     { this.symbol = v; return this; }
        public Builder companyName(String v)                { this.companyName = v; return this; }
        public Builder catalystType(CandidateCatalystType v){ this.catalystType = v; return this; }
        public Builder sourceType(SourceType v)             { this.sourceType = v; return this; }
        public Builder sourceId(String v)                   { this.sourceId = v; return this; }
        public Builder sourceUrl(String v)                  { this.sourceUrl = v; return this; }
        public Builder observedAt(Instant v)                { this.observedAt = v; return this; }
        public Builder headline(String v)                   { this.headline = v; return this; }
        public Builder summary(String v)                    { this.summary = v; return this; }
        public Builder structuralRealityScore(double v)     { this.structuralRealityScore = v; return this; }
        public Builder materialSignificanceScore(double v)  { this.materialSignificanceScore = v; return this; }
        public Builder earlynessScore(double v)             { this.earlynessScore = v; return this; }
        public Builder reflexivityPotentialScore(double v)  { this.reflexivityPotentialScore = v; return this; }

        public TradeCandidate build() {
            return new TradeCandidate(
                    candidateId, symbol, companyName, catalystType, sourceType,
                    sourceId, sourceUrl, observedAt, headline, summary,
                    structuralRealityScore, materialSignificanceScore,
                    earlynessScore, reflexivityPotentialScore
            );
        }
    }
}
