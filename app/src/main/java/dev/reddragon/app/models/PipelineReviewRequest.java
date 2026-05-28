package dev.reddragon.app.models;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.validation.config.ValidationProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PipelineReviewRequest {
    @NotBlank(message = "symbol is required")
    private String symbol;

    @NotBlank(message = "companyName is required")
    private String companyName;

    @NotNull(message = "catalystType is required")
    private CandidateCatalystType catalystType;

    @NotBlank(message = "headline is required")
    private String headline;

    @NotBlank(message = "summary is required")
    private String summary;

    @NotNull(message = "structuralRealityScore is required")
    @DecimalMin(value = "0.0", message = "structuralRealityScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "structuralRealityScore must be between 0.0 and 1.0")
    private Double structuralRealityScore;

    @NotNull(message = "materialSignificanceScore is required")
    @DecimalMin(value = "0.0", message = "materialSignificanceScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "materialSignificanceScore must be between 0.0 and 1.0")
    private Double materialSignificanceScore;

    @NotNull(message = "earlynessScore is required")
    @DecimalMin(value = "0.0", message = "earlynessScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "earlynessScore must be between 0.0 and 1.0")
    private Double earlynessScore;

    @NotNull(message = "reflexivityPotentialScore is required")
    @DecimalMin(value = "0.0", message = "reflexivityPotentialScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "reflexivityPotentialScore must be between 0.0 and 1.0")
    private Double reflexivityPotentialScore;

    private LocalDate marketDataFrom;
    private LocalDate marketDataTo;

    @Valid
    private List<BarRequest> bars;

    /** Validation strictness profile. Defaults to STANDARD when omitted. */
    private ValidationProfile profile;

    @AssertTrue(message = "marketDataFrom and marketDataTo must both be provided together")
    public boolean isMarketDataRangeComplete() {
        return (marketDataFrom == null) == (marketDataTo == null);
    }

    @AssertTrue(message = "marketDataFrom must be on or before marketDataTo")
    public boolean isMarketDataRangeOrdered() {
        return marketDataFrom == null || marketDataTo == null || !marketDataFrom.isAfter(marketDataTo);
    }
}
