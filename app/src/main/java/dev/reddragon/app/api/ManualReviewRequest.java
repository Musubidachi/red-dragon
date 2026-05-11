package dev.reddragon.app.api;

import dev.reddragon.ingestion.model.CandidateCatalystType;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
@Accessors(fluent = true)
public class ManualReviewRequest {
    String symbol;
    String companyName;
    CandidateCatalystType catalystType;
    String headline;
    String summary;
    double structuralRealityScore;
    double materialSignificanceScore;
    double earlynessScore;
    double reflexivityPotentialScore;
    List<BarRequest> bars;

    @Value
    @Builder
    @Accessors(fluent = true)
    public static class BarRequest {
        LocalDate date;
        double open;
        double high;
        double low;
        double close;
        long volume;
    }
}
