package dev.reddragon.app.models;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.validation.config.ValidationProfile;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PipelineReviewRequest {
    private String symbol;
    private String companyName;
    private CandidateCatalystType catalystType;
    private String headline;
    private String summary;
    private double structuralRealityScore;
    private double materialSignificanceScore;
    private double earlynessScore;
    private double reflexivityPotentialScore;
    private LocalDate marketDataFrom;
    private LocalDate marketDataTo;
    private List<BarRequest> bars;

    /** Validation strictness profile. Defaults to STANDARD when omitted. */
    private ValidationProfile profile;
}
