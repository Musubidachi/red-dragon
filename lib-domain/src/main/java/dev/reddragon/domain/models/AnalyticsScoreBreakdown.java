package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
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
        this.structuralRealityScore = DomainScorePolicy.clampDerivedScore(structuralRealityScore);
        this.materialSignificanceScore = DomainScorePolicy.clampDerivedScore(materialSignificanceScore);
        this.earlynessScore = DomainScorePolicy.clampDerivedScore(earlynessScore);
        this.equilibriumQualityScore = DomainScorePolicy.clampDerivedScore(equilibriumQualityScore);
        this.reflexivityPotentialScore = DomainScorePolicy.clampDerivedScore(reflexivityPotentialScore);
        this.asymmetryScore = DomainScorePolicy.clampDerivedScore(asymmetryScore);
        this.regimeCompatibilityScore = DomainScorePolicy.clampDerivedScore(regimeCompatibilityScore);
        this.deploymentConfidenceScore = DomainScorePolicy.clampDerivedScore(deploymentConfidenceScore);
    }

    /** Average of all 8 dimension scores. */
    public double average() {
        return (structuralRealityScore + materialSignificanceScore + earlynessScore
                + equilibriumQualityScore + reflexivityPotentialScore
                + asymmetryScore + regimeCompatibilityScore + deploymentConfidenceScore) / 8.0;
    }

    /**
     * Dimension with the lowest score - the weakest signal.
     *
     * <p>Every comparison updates both {@code min} and {@code name} for
     * symmetry; adding a new dimension at the end of the chain does not
     * require remembering to special-case the previous last line.
     */
    public ScoreDimension weakestDimension() {
        double min = structuralRealityScore;
        ScoreDimension name = ScoreDimension.STRUCTURAL_REALITY;
        if (materialSignificanceScore < min)  { min = materialSignificanceScore;  name = ScoreDimension.MATERIAL_SIGNIFICANCE; }
        if (earlynessScore < min)             { min = earlynessScore;             name = ScoreDimension.EARLYNESS; }
        if (equilibriumQualityScore < min)    { min = equilibriumQualityScore;    name = ScoreDimension.EQUILIBRIUM_QUALITY; }
        if (reflexivityPotentialScore < min)  { min = reflexivityPotentialScore;  name = ScoreDimension.REFLEXIVITY_POTENTIAL; }
        if (asymmetryScore < min)             { min = asymmetryScore;             name = ScoreDimension.ASYMMETRY; }
        if (regimeCompatibilityScore < min)   { min = regimeCompatibilityScore;   name = ScoreDimension.REGIME_COMPATIBILITY; }
        if (deploymentConfidenceScore < min)  { min = deploymentConfidenceScore;  name = ScoreDimension.DEPLOYMENT_CONFIDENCE; }
        return name;
    }

    /**
     * Dimension with the highest score - the strongest signal.
     *
     * <p>Every comparison updates both {@code max} and {@code name} for
     * symmetry; see {@link #weakestDimension()} for the rationale.
     */
    public ScoreDimension strongestDimension() {
        double max = structuralRealityScore;
        ScoreDimension name = ScoreDimension.STRUCTURAL_REALITY;
        if (materialSignificanceScore > max)  { max = materialSignificanceScore;  name = ScoreDimension.MATERIAL_SIGNIFICANCE; }
        if (earlynessScore > max)             { max = earlynessScore;             name = ScoreDimension.EARLYNESS; }
        if (equilibriumQualityScore > max)    { max = equilibriumQualityScore;    name = ScoreDimension.EQUILIBRIUM_QUALITY; }
        if (reflexivityPotentialScore > max)  { max = reflexivityPotentialScore;  name = ScoreDimension.REFLEXIVITY_POTENTIAL; }
        if (asymmetryScore > max)             { max = asymmetryScore;             name = ScoreDimension.ASYMMETRY; }
        if (regimeCompatibilityScore > max)   { max = regimeCompatibilityScore;   name = ScoreDimension.REGIME_COMPATIBILITY; }
        if (deploymentConfidenceScore > max)  { max = deploymentConfidenceScore;  name = ScoreDimension.DEPLOYMENT_CONFIDENCE; }
        return name;
    }
}
