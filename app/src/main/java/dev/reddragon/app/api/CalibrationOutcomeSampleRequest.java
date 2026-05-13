package dev.reddragon.app.api;

import lombok.Data;

/**
 * One historical trade outcome submitted for calibration analysis.
 *
 * <p>Fields map directly to {@link dev.reddragon.analytics.model.OutcomeSample}.
 * The {@code scoreBreakdown} fields should come from the analytics snapshot
 * that was produced when the candidate was originally reviewed.
 */
@Data
public class CalibrationOutcomeSampleRequest {

    private String candidateId;
    private String symbol;

    /** Realized return from entry to exit, as a decimal (e.g. 0.12 = 12%). */
    private double realizedReturn;

    /** Maximum adverse excursion from entry, as a decimal (e.g. 0.08 = 8% drawdown). */
    private double maxDrawdown;

    /** Number of calendar days the position was held. */
    private double daysHeld;

    /** Whether the original thesis ultimately played out as expected. */
    private boolean thesisWorked;

    // Score breakdown at the time of the original review.
    private double structuralRealityScore;
    private double materialSignificanceScore;
    private double earlynessScore;
    private double equilibriumQualityScore;
    private double reflexivityPotentialScore;
    private double asymmetryScore;
    private double regimeCompatibilityScore;
    private double deploymentConfidenceScore;
}
