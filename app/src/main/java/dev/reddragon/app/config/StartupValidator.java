package dev.reddragon.app.config;

import dev.reddragon.validation.config.ValidationThresholds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs at startup to validate the active configuration and surface
 * mis-configurations before the first request is processed.
 *
 * <p>Failures are logged as warnings — the application still starts so that
 * the H2 console and actuator health endpoints remain reachable for diagnosis.
 */
@Component
public class StartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    private final ValidationThresholds thresholds;

    public StartupValidator(ValidationThresholds thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("StartupValidator: checking active configuration…");

        validateThreshold("passThreshold",            thresholds.getPassThreshold(),            0.5, 1.0);
        validateThreshold("watchThreshold",           thresholds.getWatchThreshold(),           0.0, thresholds.getPassThreshold());
        validateThreshold("concentrationThreshold",   thresholds.getConcentrationThreshold(),   thresholds.getPassThreshold(), 1.0);
        validateThreshold("minStructuralReality",     thresholds.getMinStructuralReality(),     0.0, 1.0);
        validateThreshold("minMaterialSignificance",  thresholds.getMinMaterialSignificance(),  0.0, 1.0);
        validateThreshold("minAsymmetry",             thresholds.getMinAsymmetry(),             0.0, 1.0);

        double weightSum = thresholds.getStructuralRealityWeight()
                + thresholds.getMaterialSignificanceWeight()
                + thresholds.getEarlynessWeight()
                + thresholds.getEquilibriumQualityWeight()
                + thresholds.getReflexivityPotentialWeight()
                + thresholds.getAsymmetryWeight()
                + thresholds.getRegimeCompatibilityWeight()
                + thresholds.getDeploymentConfidenceWeight();

        if (Math.abs(weightSum - 1.0) > 0.01) {
            log.warn("StartupValidator: scoring weights sum to {:.4f} — expected 1.0; review red-dragon.validation weights", weightSum);
        } else {
            log.info("StartupValidator: scoring weights sum to {:.4f} ✓", weightSum);
        }

        log.info("StartupValidator: configuration check complete");
    }

    private void validateThreshold(String name, double value, double min, double max) {
        if (value < min || value > max) {
            log.warn("StartupValidator: {} = {} is outside expected range [{}, {}]", name, value, min, max);
        }
    }
}
