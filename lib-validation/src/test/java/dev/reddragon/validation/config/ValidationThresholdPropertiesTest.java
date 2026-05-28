package dev.reddragon.validation.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationThresholdPropertiesTest {

    private static final double EPSILON = 0.000001;

    @Test
    void bindsKebabCaseOverridesAndKeepsStandardDefaultsForOmittedValues() {
        ValidationThresholdProperties properties = bind(Map.of(
                "red-dragon.validation.pass-threshold", "0.82",
                "red-dragon.validation.concentration-asymmetry-threshold", "0.81",
                "red-dragon.validation.standard-deployment-confidence-threshold", "0.70",
                "red-dragon.validation.probe-deployment-confidence-threshold", "0.50"
        ));

        ValidationThresholds thresholds = properties.toThresholds();
        ValidationThresholds defaults = ValidationThresholds.defaults();

        assertEquals(0.82, thresholds.passThreshold(), EPSILON);
        assertEquals(0.81, thresholds.concentrationAsymmetryThreshold(), EPSILON);
        assertEquals(0.70, thresholds.standardDeploymentConfidenceThreshold(), EPSILON);
        assertEquals(0.50, thresholds.probeDeploymentConfidenceThreshold(), EPSILON);
        assertEquals(defaults.watchThreshold(), thresholds.watchThreshold(), EPSILON);
        assertEquals(defaults.totalWeight(), thresholds.totalWeight(), EPSILON);
    }

    @Test
    void validatesBoundThresholdOrderingWhenMaterialized() {
        ValidationThresholdProperties properties = bind(Map.of(
                "red-dragon.validation.probe-deployment-confidence-threshold", "0.71",
                "red-dragon.validation.standard-deployment-confidence-threshold", "0.70"
        ));

        assertThrows(IllegalArgumentException.class, properties::toThresholds);
    }

    private ValidationThresholdProperties bind(Map<String, String> values) {
        Binder binder = new Binder(new MapConfigurationPropertySource(values));
        return binder.bind(
                ValidationThresholdProperties.PREFIX,
                ValidationThresholdProperties.class
        ).orElseThrow(() -> new AssertionError("validation threshold properties should bind"));
    }
}
