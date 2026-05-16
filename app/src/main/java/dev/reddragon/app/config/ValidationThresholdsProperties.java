package dev.reddragon.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.reddragon.validation.config.ValidationThresholds;
import lombok.Data;

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
@Data
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
}
