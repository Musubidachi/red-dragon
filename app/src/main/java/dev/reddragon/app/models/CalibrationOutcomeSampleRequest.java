package dev.reddragon.app.models;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * One historical trade outcome submitted for calibration analysis.
 *
 * <p>Fields map directly to {@link dev.reddragon.domain.models.OutcomeSample}.
 * The {@code scoreBreakdown} fields should come from the analytics snapshot
 * that was produced when the candidate was originally reviewed.
 */
@Data
public class CalibrationOutcomeSampleRequest {

    private String candidateId;

    @NotBlank(message = "symbol is required")
    private String symbol;

    /** Realized return from entry to exit, as a decimal (e.g. 0.12 = 12%). */
    @NotNull(message = "realizedReturn is required")
    private Double realizedReturn;

    /** Maximum adverse excursion from entry, as a decimal (e.g. 0.08 = 8% drawdown). */
    @NotNull(message = "maxDrawdown is required")
    @DecimalMin(value = "0.0", message = "maxDrawdown must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "maxDrawdown must be between 0.0 and 1.0")
    private Double maxDrawdown;

    /** Number of calendar days the position was held. */
    @NotNull(message = "daysHeld is required")
    @PositiveOrZero(message = "daysHeld must be zero or greater")
    private Integer daysHeld;

    /** Whether the original thesis ultimately played out as expected. */
    private boolean thesisWorked;

    // Score breakdown at the time of the original review.
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

    @NotNull(message = "equilibriumQualityScore is required")
    @DecimalMin(value = "0.0", message = "equilibriumQualityScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "equilibriumQualityScore must be between 0.0 and 1.0")
    private Double equilibriumQualityScore;

    @NotNull(message = "reflexivityPotentialScore is required")
    @DecimalMin(value = "0.0", message = "reflexivityPotentialScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "reflexivityPotentialScore must be between 0.0 and 1.0")
    private Double reflexivityPotentialScore;

    @NotNull(message = "asymmetryScore is required")
    @DecimalMin(value = "0.0", message = "asymmetryScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "asymmetryScore must be between 0.0 and 1.0")
    private Double asymmetryScore;

    @NotNull(message = "regimeCompatibilityScore is required")
    @DecimalMin(value = "0.0", message = "regimeCompatibilityScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "regimeCompatibilityScore must be between 0.0 and 1.0")
    private Double regimeCompatibilityScore;

    @NotNull(message = "deploymentConfidenceScore is required")
    @DecimalMin(value = "0.0", message = "deploymentConfidenceScore must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "deploymentConfidenceScore must be between 0.0 and 1.0")
    private Double deploymentConfidenceScore;
}
