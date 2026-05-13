package dev.reddragon.app.config;

import dev.reddragon.validation.config.ValidationThresholds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs at startup to validate the active configuration and surface
 * misconfigurations before the first request is processed.
 *
 * <p>Configuration errors are logged as warnings so actuator and diagnostic
 * endpoints remain available. The {@link ValidationThresholds} constructor
 * still enforces hard validity rules for impossible values.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupValidator implements ApplicationRunner {

    private final ValidationThresholds thresholds;

    @Override
    public void run(ApplicationArguments args) {
        log.info("StartupValidator: checking active configuration");

        validateThreshold("passThreshold", thresholds.passThreshold(), 0.5, 1.0);
        validateThreshold("watchThreshold", thresholds.watchThreshold(), 0.0, thresholds.passThreshold());
        validateThreshold("concentrationThreshold", thresholds.concentrationThreshold(), thresholds.passThreshold(), 1.0);
        validateThreshold("minStructuralReality", thresholds.minStructuralReality(), 0.0, 1.0);
        validateThreshold("minMaterialSignificance", thresholds.minMaterialSignificance(), 0.0, 1.0);
        validateThreshold("minAsymmetry", thresholds.minAsymmetry(), 0.0, 1.0);

        double weightSum = thresholds.totalWeight();

        if (Math.abs(weightSum - 1.0) > 0.01) {
            log.warn("StartupValidator: scoring weights sum to {} — expected 1.0; review red-dragon.validation weights", weightSum);
        } else {
            log.info("StartupValidator: scoring weights sum to {}", weightSum);
        }

        log.info("StartupValidator: configuration check complete");
    }

    private void validateThreshold(String name, double value, double min, double max) {
        if (value < min || value > max) {
            log.warn("StartupValidator: {} = {} is outside expected range [{}, {}]", name, value, min, max);
        }
    }
}
