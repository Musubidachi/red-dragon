package dev.reddragon.app.config;

import dev.reddragon.validation.config.ValidationThresholds;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@code red-dragon.validation.*} from {@code application.yml} to a
 * {@link ValidationThresholds} instance.
 *
 * <p>All keys are optional — any omitted key falls back to the STANDARD default.
 * This lets you tune a single threshold without having to specify the full set.
 *
 * <pre>
 * red-dragon:
 *   validation:
 *     pass-threshold: 0.80          # tighter pass gate
 *     watch-threshold: 0.60
 *     min-structural-reality: 0.70
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "red-dragon.validation")
public class ValidationThresholdsProperties {

    // Verdict thresholds
    private double passThreshold = 0.78;
    private double watchThreshold = 0.58;
    private double concentrationThreshold = 0.87;
    private double standardDeploymentThreshold = 0.78;
    private double probeDeploymentThreshold = 0.58;

    // Minimum required scores (hard gate)
    private double minStructuralReality = 0.65;
    private double minMaterialSignificance = 0.55;
    private double minEarlyness = 0.45;
    private double minEquilibriumQuality = 0.45;
    private double minAsymmetry = 0.55;
    private double minRegimeCompatibility = 0.40;

    // Scoring weights
    private double structuralRealityWeight = 0.18;
    private double materialSignificanceWeight = 0.13;
    private double earlynessWeight = 0.16;
    private double equilibriumQualityWeight = 0.12;
    private double reflexivityPotentialWeight = 0.11;
    private double asymmetryWeight = 0.18;
    private double regimeCompatibilityWeight = 0.07;
    private double deploymentConfidenceWeight = 0.05;

    @Bean
    public ValidationThresholds defaultThresholds() {
        return new ValidationThresholds(
                passThreshold,
                watchThreshold,
                concentrationThreshold,
                standardDeploymentThreshold,
                probeDeploymentThreshold,
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

    // Getters and setters (required by @ConfigurationProperties binding)

    public double getPassThreshold() { return passThreshold; }
    public void setPassThreshold(double v) { this.passThreshold = v; }

    public double getWatchThreshold() { return watchThreshold; }
    public void setWatchThreshold(double v) { this.watchThreshold = v; }

    public double getConcentrationThreshold() { return concentrationThreshold; }
    public void setConcentrationThreshold(double v) { this.concentrationThreshold = v; }

    public double getStandardDeploymentThreshold() { return standardDeploymentThreshold; }
    public void setStandardDeploymentThreshold(double v) { this.standardDeploymentThreshold = v; }

    public double getProbeDeploymentThreshold() { return probeDeploymentThreshold; }
    public void setProbeDeploymentThreshold(double v) { this.probeDeploymentThreshold = v; }

    public double getMinStructuralReality() { return minStructuralReality; }
    public void setMinStructuralReality(double v) { this.minStructuralReality = v; }

    public double getMinMaterialSignificance() { return minMaterialSignificance; }
    public void setMinMaterialSignificance(double v) { this.minMaterialSignificance = v; }

    public double getMinEarlyness() { return minEarlyness; }
    public void setMinEarlyness(double v) { this.minEarlyness = v; }

    public double getMinEquilibriumQuality() { return minEquilibriumQuality; }
    public void setMinEquilibriumQuality(double v) { this.minEquilibriumQuality = v; }

    public double getMinAsymmetry() { return minAsymmetry; }
    public void setMinAsymmetry(double v) { this.minAsymmetry = v; }

    public double getMinRegimeCompatibility() { return minRegimeCompatibility; }
    public void setMinRegimeCompatibility(double v) { this.minRegimeCompatibility = v; }

    public double getStructuralRealityWeight() { return structuralRealityWeight; }
    public void setStructuralRealityWeight(double v) { this.structuralRealityWeight = v; }

    public double getMaterialSignificanceWeight() { return materialSignificanceWeight; }
    public void setMaterialSignificanceWeight(double v) { this.materialSignificanceWeight = v; }

    public double getEarlynessWeight() { return earlynessWeight; }
    public void setEarlynessWeight(double v) { this.earlynessWeight = v; }

    public double getEquilibriumQualityWeight() { return equilibriumQualityWeight; }
    public void setEquilibriumQualityWeight(double v) { this.equilibriumQualityWeight = v; }

    public double getReflexivityPotentialWeight() { return reflexivityPotentialWeight; }
    public void setReflexivityPotentialWeight(double v) { this.reflexivityPotentialWeight = v; }

    public double getAsymmetryWeight() { return asymmetryWeight; }
    public void setAsymmetryWeight(double v) { this.asymmetryWeight = v; }

    public double getRegimeCompatibilityWeight() { return regimeCompatibilityWeight; }
    public void setRegimeCompatibilityWeight(double v) { this.regimeCompatibilityWeight = v; }

    public double getDeploymentConfidenceWeight() { return deploymentConfidenceWeight; }
    public void setDeploymentConfidenceWeight(double v) { this.deploymentConfidenceWeight = v; }
}
