package dev.reddragon.app.api;

import dev.reddragon.ingestion.model.CandidateCatalystType;

import java.time.LocalDate;
import java.util.List;

public record ManualReviewRequest(
        String symbol,
        String companyName,
        CandidateCatalystType catalystType,
        String headline,
        String summary,
        double structuralRealityScore,
        double materialSignificanceScore,
        double earlynessScore,
        double reflexivityPotentialScore,
        List<BarRequest> bars
) {
    public record BarRequest(
            LocalDate date,
            double open,
            double high,
            double low,
            double close,
            long volume
    ) {
    }
}
