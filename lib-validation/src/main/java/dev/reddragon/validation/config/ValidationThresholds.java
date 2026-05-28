package dev.reddragon.validation.config;

import dev.reddragon.math.ValidationScoreUtils;
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
    /**
     * Lower edge of the OBSERVE deployment band. Scores in
     * {@code [observeDeploymentThreshold, probeDeploymentThreshold)} produce
     * the OBSERVE tier. When score clears the PROBE aggregate threshold but
     * deployment confidence does not clear the PROBE confidence floor, the
     * candidate also downgrades to OBSERVE rather than becoming actionable.
     */
    double observeDeploymentThreshold;
    double concentrationDeploymentConfidenceThreshold;
    double concentrationAsymmetryThreshold;
    double concentrationEarlynessThreshold;
    double standardDeploymentConfidenceThreshold;
    double probeDeploymentConfidenceThreshold;

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
                0.58,  // observeDeploymentThreshold
                0.80,  // concentrationDeploymentConfidenceThreshold
                0.78,  // concentrationAsymmetryThreshold
                0.78,  // concentrationEarlynessThreshold
                0.65,  // standardDeploymentConfidenceThreshold
                0.45,  // probeDeploymentConfidenceThreshold

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
            double observeDeploymentThreshold,
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
        this(
                passThreshold,
                watchThreshold,
                concentrationThreshold,
                standardDeploymentThreshold,
                probeDeploymentThreshold,
                observeDeploymentThreshold,
                standardDeploymentThreshold,
                passThreshold,
                passThreshold,
                Math.min(0.65, standardDeploymentThreshold),
                Math.min(0.45, standardDeploymentThreshold),
                minStructuralReality,
                minMaterialSignificance,
                minEarlyness,
                minEquilibriumQuality,
                minAsymmetry,
                minRegimeCompatibility,
                structuralRealityWeight,
                materialSignificanceWeight,
                earlynessWeight,
                equilibriumQualityWeight,
                reflexivityPotentialWeight,
                asymmetryWeight,
                regimeCompatibilityWeight,
                deploymentConfidenceWeight
        );
    }

    public ValidationThresholds(
            double passThreshold,
            double watchThreshold,
            double concentrationThreshold,
            double standardDeploymentThreshold,
            double probeDeploymentThreshold,
            double observeDeploymentThreshold,
            double concentrationDeploymentConfidenceThreshold,
            double concentrationAsymmetryThreshold,
            double concentrationEarlynessThreshold,
            double standardDeploymentConfidenceThreshold,
            double probeDeploymentConfidenceThreshold,
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
        ValidationScoreUtils.requireNormalized("observeDeploymentThreshold", observeDeploymentThreshold);
        ValidationScoreUtils.requireNormalized(
                "concentrationDeploymentConfidenceThreshold",
                concentrationDeploymentConfidenceThreshold);
        ValidationScoreUtils.requireNormalized("concentrationAsymmetryThreshold", concentrationAsymmetryThreshold);
        ValidationScoreUtils.requireNormalized("concentrationEarlynessThreshold", concentrationEarlynessThreshold);
        ValidationScoreUtils.requireNormalized(
                "standardDeploymentConfidenceThreshold",
                standardDeploymentConfidenceThreshold);
        ValidationScoreUtils.requireNormalized("probeDeploymentConfidenceThreshold", probeDeploymentConfidenceThreshold);
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
        if (observeDeploymentThreshold > probeDeploymentThreshold) {
            throw new IllegalArgumentException(
                    "observeDeploymentThreshold cannot be greater than probeDeploymentThreshold");
        }
        if (probeDeploymentThreshold > standardDeploymentThreshold) {
            throw new IllegalArgumentException(
                    "probeDeploymentThreshold cannot be greater than standardDeploymentThreshold");
        }
        if (standardDeploymentThreshold > concentrationThreshold) {
            throw new IllegalArgumentException(
                    "standardDeploymentThreshold cannot be greater than concentrationThreshold");
        }
        if (passThreshold > concentrationThreshold) {
            throw new IllegalArgumentException(
                    "passThreshold cannot be greater than concentrationThreshold");
        }
        if (standardDeploymentConfidenceThreshold > concentrationDeploymentConfidenceThreshold) {
            throw new IllegalArgumentException(
                    "standardDeploymentConfidenceThreshold cannot be greater than "
                            + "concentrationDeploymentConfidenceThreshold");
        }
        if (probeDeploymentConfidenceThreshold > standardDeploymentConfidenceThreshold) {
            throw new IllegalArgumentException(
                    "probeDeploymentConfidenceThreshold cannot be greater than "
                            + "standardDeploymentConfidenceThreshold");
        }

        this.passThreshold = passThreshold;
        this.watchThreshold = watchThreshold;
        this.concentrationThreshold = concentrationThreshold;
        this.standardDeploymentThreshold = standardDeploymentThreshold;
        this.probeDeploymentThreshold = probeDeploymentThreshold;
        this.observeDeploymentThreshold = observeDeploymentThreshold;
        this.concentrationDeploymentConfidenceThreshold = concentrationDeploymentConfidenceThreshold;
        this.concentrationAsymmetryThreshold = concentrationAsymmetryThreshold;
        this.concentrationEarlynessThreshold = concentrationEarlynessThreshold;
        this.standardDeploymentConfidenceThreshold = standardDeploymentConfidenceThreshold;
        this.probeDeploymentConfidenceThreshold = probeDeploymentConfidenceThreshold;
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
