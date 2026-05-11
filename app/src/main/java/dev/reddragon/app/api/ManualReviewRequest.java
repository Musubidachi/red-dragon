package dev.reddragon.app.api;

import dev.reddragon.ingestion.model.CandidateCatalystType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewRequest {
    private String symbol;
    private String companyName;
    private CandidateCatalystType catalystType;
    private String headline;
    private String summary;
    private double structuralRealityScore;
    private double materialSignificanceScore;
    private double earlynessScore;
    private double reflexivityPotentialScore;
    private List<BarRequest> bars;
}
