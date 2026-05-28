package dev.reddragon.validation.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationThresholdProfileFactoryTest {

    private static final double EPSILON = 0.000001;

    @Test
    void standardProfileMatchesDefaultThresholds() {
        assertEquals(
                ValidationThresholds.defaults(),
                ValidationThresholdProfileFactory.process(ValidationProfile.STANDARD)
        );
    }

    @Test
    void everyProfileHasValidOrderingAndPositiveWeights() {
        for (ValidationProfile profile : ValidationProfile.values()) {
            ValidationThresholds thresholds = ValidationThresholdProfileFactory.process(profile);

            assertTrue(thresholds.totalWeight() > 0.0, profile.name());
            assertTrue(thresholds.watchThreshold() <= thresholds.passThreshold(), profile.name());
            assertTrue(thresholds.passThreshold() <= thresholds.concentrationThreshold(), profile.name());
            assertTrue(
                    thresholds.observeDeploymentThreshold() <= thresholds.probeDeploymentThreshold(),
                    profile.name());
            assertTrue(
                    thresholds.probeDeploymentThreshold() <= thresholds.standardDeploymentThreshold(),
                    profile.name());
            assertTrue(
                    thresholds.standardDeploymentThreshold() <= thresholds.concentrationThreshold(),
                    profile.name());
            assertTrue(
                    thresholds.probeDeploymentConfidenceThreshold()
                            <= thresholds.standardDeploymentConfidenceThreshold(),
                    profile.name());
            assertTrue(
                    thresholds.standardDeploymentConfidenceThreshold()
                            <= thresholds.concentrationDeploymentConfidenceThreshold(),
                    profile.name());
        }
    }

    @Test
    void profilesKeepDeploymentConfidenceGatesSeparateFromAggregateThresholds() {
        for (ValidationProfile profile : ValidationProfile.values()) {
            ValidationThresholds thresholds = ValidationThresholdProfileFactory.process(profile);

            assertTrue(thresholds.probeDeploymentConfidenceThreshold() > 0.0, profile.name());
            assertTrue(thresholds.standardDeploymentConfidenceThreshold() > 0.0, profile.name());
            assertEquals(
                    thresholds.passThreshold(),
                    thresholds.concentrationAsymmetryThreshold(),
                    EPSILON,
                    profile.name());
            assertEquals(
                    thresholds.passThreshold(),
                    thresholds.concentrationEarlynessThreshold(),
                    EPSILON,
                    profile.name());
        }
    }
}
