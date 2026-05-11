package dev.reddragon.validation.config;

import dev.reddragon.validation.util.ValidationScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Tunable thresholds and weights for the disequilibrium validation engine.
 *
 * Defaults are intentionally strict because this framework is designed for
 * selective participation and possible concentration, not broad scanning.
 */
@Value
@Accessors(fluent = true)
public class ValidationThresholds {
    double passThreshold;
    double watchThreshold;
    double concentrationThreshold;
    double standardDeploymentThreshold;
    double probeDeploymentThreshold;

    double minStructuralReality;
    double minMaterialSignificance;
    double minEarlyness;
    double minEquilibriumQuality;
    double minAsymmetry;
    double minRegimeCompatibility;

    double structuralRealityWeight;
    double materialSignificanceWeight;
    double earlynessWeight;
    double equilibriumQualityWeight;
    double reflexivityPotentialWeight;
    double asymmetryWeight;
    double regimeCompatibilityWeight;
    double deploymentConfidenceWeight;

    public static ValidationThresholds defaults() {
        return new ValidationThresholds(
                0.78,
                0.58,
                0.87,
                0.78,
                0.58,

                0.65,
                0.55,
                0.45,
                0.45,
                0.55,
                0.40,

                0.18,
                0.13,
                0.16,
                0.12,
                0.11,
                0.18,
                0.07,
                0.05
        );
    }

    public ValidationThresholds(
            double passThreshold,
            double watchThreshold,
            double concentrationThreshold,
            double standardDeploymentThreshold,
            double probeDeploymentThreshold,
            double minStructuralReality,
            double minMaterialSignificance,
            double minEarlyness,
            double minEquilibriumQuality,
            double minAsymmetry,
            double minRegimeCompatibility,
            double structuralRealityWeight,
            double materialSignificanceWeight,
            double earlynessWeight,
            double equilibriumQualityWeight,
            double reflexivityPotentialWeight,
            double asymmetryWeight,
            double regimeCompatibilityWeight,
            double deploymentConfidenceWeight
    ) {
        ValidationScoreUtils.requireNormalized("passThreshold", passThreshold);
        ValidationScoreUtils.requireNormalized("watchThreshold", watchThreshold);
        ValidationScoreUtils.requireNormalized("concentrationThreshold", concentrationThreshold);
        ValidationScoreUtils.requireNormalized("standardDeploymentThreshold", standardDeploymentThreshold);
        ValidationScoreUtils.requireNormalized("probeDeploymentThreshold", probeDeploymentThreshold);
        ValidationScoreUtils.requireNormalized("minStructuralReality", minStructuralReality);
        ValidationScoreUtils.requireNormalized("minMaterialSignificance", minMaterialSignificance);
        ValidationScoreUtils.requireNormalized("minEarlyness", minEarlyness);
        ValidationScoreUtils.requireNormalized("minEquilibriumQuality", minEquilibriumQuality);
        ValidationScoreUtils.requireNormalized("minAsymmetry", minAsymmetry);
        ValidationScoreUtils.requireNormalized("minRegimeCompatibility", minRegimeCompatibility);
        ValidationScoreUtils.requireNonNegative("structuralRealityWeight", structuralRealityWeight);
        ValidationScoreUtils.requireNonNegative("materialSignificanceWeight", materialSignificanceWeight);
        ValidationScoreUtils.requireNonNegative("earlynessWeight", earlynessWeight);
        ValidationScoreUtils.requireNonNegative("equilibriumQualityWeight", equilibriumQualityWeight);
        ValidationScoreUtils.requireNonNegative("reflexivityPotentialWeight", reflexivityPotentialWeight);
        ValidationScoreUtils.requireNonNegative("asymmetryWeight", asymmetryWeight);
        ValidationScoreUtils.requireNonNegative("regimeCompatibilityWeight", regimeCompatibilityWeight);
        ValidationScoreUtils.requireNonNegative("deploymentConfidenceWeight", deploymentConfidenceWeight);

        double weightTotal = structuralRealityWeight
                + materialSignificanceWeight
                + earlynessWeight
                + equilibriumQualityWeight
                + reflexivityPotentialWeight
                + asymmetryWeight
                + regimeCompatibilityWeight
                + deploymentConfidenceWeight;
        if (weightTotal <= 0.0) {
            throw new IllegalArgumentException("At least one validation weight must be positive");
        }
        if (watchThreshold > passThreshold) {
            throw new IllegalArgumentException("watchThreshold cannot be greater than passThreshold");
        }

        this.passThreshold = passThreshold;
        this.watchThreshold = watchThreshold;
        this.concentrationThreshold = concentrationThreshold;
        this.standardDeploymentThreshold = standardDeploymentThreshold;
        this.probeDeploymentThreshold = probeDeploymentThreshold;
        this.minStructuralReality = minStructuralReality;
        this.minMaterialSignificance = minMaterialSignificance;
        this.minEarlyness = minEarlyness;
        this.minEquilibriumQuality = minEquilibriumQuality;
        this.minAsymmetry = minAsymmetry;
        this.minRegimeCompatibility = minRegimeCompatibility;
        this.structuralRealityWeight = structuralRealityWeight;
        this.materialSignificanceWeight = materialSignificanceWeight;
        this.earlynessWeight = earlynessWeight;
        this.equilibriumQualityWeight = equilibriumQualityWeight;
        this.reflexivityPotentialWeight = reflexivityPotentialWeight;
        this.asymmetryWeight = asymmetryWeight;
        this.regimeCompatibilityWeight = regimeCompatibilityWeight;
        this.deploymentConfidenceWeight = deploymentConfidenceWeight;
    }

    public double totalWeight() {
        return structuralRealityWeight
                + materialSignificanceWeight
                + earlynessWeight
                + equilibriumQualityWeight
                + reflexivityPotentialWeight
                + asymmetryWeight
                + regimeCompatibilityWeight
                + deploymentConfidenceWeight;
    }
}
