package dev.reddragon.validation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Bindable {@code red-dragon.validation.*} threshold overrides.
 *
 * <p>All fields default to the STANDARD profile. Applications can bind this
 * class directly or subclass it from their wiring layer when they need to
 * expose a named {@link ValidationThresholds} bean.
 */
@ConfigurationProperties(prefix = ValidationThresholdProperties.PREFIX)
@Data
public class ValidationThresholdProperties {

    public static final String PREFIX = "red-dragon.validation";

    private static final ValidationThresholds STANDARD = ValidationThresholds.defaults();

    private double passThreshold = STANDARD.passThreshold();
    private double watchThreshold = STANDARD.watchThreshold();
    private double concentrationThreshold = STANDARD.concentrationThreshold();
    private double standardDeploymentThreshold = STANDARD.standardDeploymentThreshold();
    private double probeDeploymentThreshold = STANDARD.probeDeploymentThreshold();
    private double observeDeploymentThreshold = STANDARD.observeDeploymentThreshold();
    private double concentrationDeploymentConfidenceThreshold =
            STANDARD.concentrationDeploymentConfidenceThreshold();
    private double concentrationAsymmetryThreshold = STANDARD.concentrationAsymmetryThreshold();
    private double concentrationEarlynessThreshold = STANDARD.concentrationEarlynessThreshold();
    private double standardDeploymentConfidenceThreshold = STANDARD.standardDeploymentConfidenceThreshold();
    private double probeDeploymentConfidenceThreshold = STANDARD.probeDeploymentConfidenceThreshold();

    private double minStructuralReality = STANDARD.minStructuralReality();
    private double minMaterialSignificance = STANDARD.minMaterialSignificance();
    private double minEarlyness = STANDARD.minEarlyness();
    private double minEquilibriumQuality = STANDARD.minEquilibriumQuality();
    private double minAsymmetry = STANDARD.minAsymmetry();
    private double minRegimeCompatibility = STANDARD.minRegimeCompatibility();

    private double structuralRealityWeight = STANDARD.structuralRealityWeight();
    private double materialSignificanceWeight = STANDARD.materialSignificanceWeight();
    private double earlynessWeight = STANDARD.earlynessWeight();
    private double equilibriumQualityWeight = STANDARD.equilibriumQualityWeight();
    private double reflexivityPotentialWeight = STANDARD.reflexivityPotentialWeight();
    private double asymmetryWeight = STANDARD.asymmetryWeight();
    private double regimeCompatibilityWeight = STANDARD.regimeCompatibilityWeight();
    private double deploymentConfidenceWeight = STANDARD.deploymentConfidenceWeight();

    public ValidationThresholds toThresholds() {
        return new ValidationThresholds(
                passThreshold,
                watchThreshold,
                concentrationThreshold,
                standardDeploymentThreshold,
                probeDeploymentThreshold,
                observeDeploymentThreshold,
                concentrationDeploymentConfidenceThreshold,
                concentrationAsymmetryThreshold,
                concentrationEarlynessThreshold,
                standardDeploymentConfidenceThreshold,
                probeDeploymentConfidenceThreshold,
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
}
