package dev.reddragon.ingestion.service;

import dev.reddragon.ingestion.model.CandidateCatalystType;
import dev.reddragon.ingestion.model.SourceType;
import dev.reddragon.ingestion.model.TradeCandidate;

import java.time.Instant;
import java.util.UUID;

/**
 * Simple deterministic ingestion adapter for manually supplied theses.
 *
 * This gives the rest of the pipeline something real to consume before external
 * adapters such as SEC EDGAR, scanners, or RSS feeds are implemented.
 */
public class ManualCandidateIngestionService {

    public TradeCandidate ingest(
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
        return TradeCandidate.builder()
                .candidateId(UUID.randomUUID().toString())
                .symbol(symbol)
                .companyName(companyName)
                .catalystType(catalystType == null ? CandidateCatalystType.MANUAL_THESIS : catalystType)
                .sourceType(SourceType.MANUAL)
                .sourceId("manual")
                .sourceUrl("")
                .observedAt(Instant.now())
                .headline(headline)
                .summary(summary)
                .structuralRealityScore(structuralRealityScore)
                .materialSignificanceScore(materialSignificanceScore)
                .earlynessScore(earlynessScore)
                .reflexivityPotentialScore(reflexivityPotentialScore)
                .build();
    }
}
