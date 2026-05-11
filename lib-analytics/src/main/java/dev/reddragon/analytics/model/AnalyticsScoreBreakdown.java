package dev.reddragon.analytics.model;

import dev.reddragon.analytics.util.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Detailed analytics scores that feed validation.
 */
@Value
@Accessors(fluent = true)
public class AnalyticsScoreBreakdown {
    double structuralRealityScore;
    double materialSignificanceScore;
    double earlynessScore;
    double equilibriumQualityScore;
    double reflexivityPotentialScore;
    double asymmetryScore;
    double regimeCompatibilityScore;
    double deploymentConfidenceScore;

    public AnalyticsScoreBreakdown(
            double structuralRealityScore,
            double materialSignificanceScore,
            double earlynessScore,
            double equilibriumQualityScore,
            double reflexivityPotentialScore,
            double asymmetryScore,
            double regimeCompatibilityScore,
            double deploymentConfidenceScore
    ) {
        this.structuralRealityScore = AnalyticsScoreUtils.clamp(structuralRealityScore);
        this.materialSignificanceScore = AnalyticsScoreUtils.clamp(materialSignificanceScore);
        this.earlynessScore = AnalyticsScoreUtils.clamp(earlynessScore);
        this.equilibriumQualityScore = AnalyticsScoreUtils.clamp(equilibriumQualityScore);
        this.reflexivityPotentialScore = AnalyticsScoreUtils.clamp(reflexivityPotentialScore);
        this.asymmetryScore = AnalyticsScoreUtils.clamp(asymmetryScore);
        this.regimeCompatibilityScore = AnalyticsScoreUtils.clamp(regimeCompatibilityScore);
        this.deploymentConfidenceScore = AnalyticsScoreUtils.clamp(deploymentConfidenceScore);
    }
}
