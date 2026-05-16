package dev.reddragon.ingestion.services;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds a trade candidate from a manually supplied thesis.
 */
public class ManualCandidateIngestionService {

    /**
     * Main processing flow.
     */
    public TradeCandidate process(
            String symbol,
            String companyName,
            CandidateCatalystType catalystType,
            String headline,
            String summary,
            double structuralRealityScore,
            double materialSignificanceScore,
            double earlynessScore,
            double reflexivityPotentialScore
    ) {
        CandidateCatalystType normalizedCatalystType = catalystType(catalystType);
        String candidateId = candidateId();
        Instant observedAt = observedAt();

        return buildCandidate(
                candidateId,
                symbol,
                companyName,
                normalizedCatalystType,
                headline,
                summary,
                observedAt,
                structuralRealityScore,
                materialSignificanceScore,
                earlynessScore,
                reflexivityPotentialScore
        );
    }

    private CandidateCatalystType catalystType(CandidateCatalystType catalystType) {
        if (catalystType == null) {
            return CandidateCatalystType.MANUAL_THESIS;
        }
        return catalystType;
    }

    private String candidateId() {
        return UUID.randomUUID().toString();
    }

    private Instant observedAt() {
        return Instant.now();
    }

    private TradeCandidate buildCandidate(
            String candidateId,
            String symbol,
            String companyName,
            CandidateCatalystType catalystType,
            String headline,
            String summary,
            Instant observedAt,
            double structuralRealityScore,
            double materialSignificanceScore,
            double earlynessScore,
            double reflexivityPotentialScore
    ) {
        return new TradeCandidate(
                candidateId,
                symbol,
                companyName,
                catalystType,
                SourceType.MANUAL,
                "manual",
                "",
                observedAt,
                headline,
                summary,
                structuralRealityScore,
                materialSignificanceScore,
                earlynessScore,
                reflexivityPotentialScore
        );
    }
}
