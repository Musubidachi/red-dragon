package dev.reddragon.app.models;

import dev.reddragon.domain.models.CandidateCatalystType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewRequest {
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

    @Valid
    private List<BarRequest> bars;
}
