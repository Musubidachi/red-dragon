package dev.reddragon.domain.models;

import dev.reddragon.math.AnalyticsScoreUtils;
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

    /** Average of all 8 dimension scores. */
    public double average() {
        return (structuralRealityScore + materialSignificanceScore + earlynessScore
                + equilibriumQualityScore + reflexivityPotentialScore
                + asymmetryScore + regimeCompatibilityScore + deploymentConfidenceScore) / 8.0;
    }

    /**
     * Name of the dimension with the lowest score — the weakest signal.
     *
     * <p>Every comparison updates both {@code min} and {@code name} for
     * symmetry; adding a new dimension at the end of the chain does not
     * require remembering to special-case the previous last line.
     */
    public String weakestDimension() {
        double min = structuralRealityScore;
        String name = "structuralReality";
        if (materialSignificanceScore < min)  { min = materialSignificanceScore;  name = "materialSignificance"; }
        if (earlynessScore < min)             { min = earlynessScore;             name = "earlyness"; }
        if (equilibriumQualityScore < min)    { min = equilibriumQualityScore;    name = "equilibriumQuality"; }
        if (reflexivityPotentialScore < min)  { min = reflexivityPotentialScore;  name = "reflexivityPotential"; }
        if (asymmetryScore < min)             { min = asymmetryScore;             name = "asymmetry"; }
        if (regimeCompatibilityScore < min)   { min = regimeCompatibilityScore;   name = "regimeCompatibility"; }
        if (deploymentConfidenceScore < min)  { min = deploymentConfidenceScore;  name = "deploymentConfidence"; }
        return name;
    }

    /**
     * Name of the dimension with the highest score — the strongest signal.
     *
     * <p>Every comparison updates both {@code max} and {@code name} for
     * symmetry; see {@link #weakestDimension()} for the rationale.
     */
    public String strongestDimension() {
        double max = structuralRealityScore;
        String name = "structuralReality";
        if (materialSignificanceScore > max)  { max = materialSignificanceScore;  name = "materialSignificance"; }
        if (earlynessScore > max)             { max = earlynessScore;             name = "earlyness"; }
        if (equilibriumQualityScore > max)    { max = equilibriumQualityScore;    name = "equilibriumQuality"; }
        if (reflexivityPotentialScore > max)  { max = reflexivityPotentialScore;  name = "reflexivityPotential"; }
        if (asymmetryScore > max)             { max = asymmetryScore;             name = "asymmetry"; }
        if (regimeCompatibilityScore > max)   { max = regimeCompatibilityScore;   name = "regimeCompatibility"; }
        if (deploymentConfidenceScore > max)  { max = deploymentConfidenceScore;  name = "deploymentConfidence"; }
        return name;
    }
}
