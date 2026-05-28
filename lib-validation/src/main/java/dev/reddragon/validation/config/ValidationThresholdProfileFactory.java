package dev.reddragon.validation.config;

/**
 * Produces threshold presets for different deployment styles.
 */
public final class ValidationThresholdProfileFactory {

    private ValidationThresholdProfileFactory() {
    }

    public static ValidationThresholds process(ValidationProfile profile) {
        return switch (profile) {
            case CONSERVATIVE -> conservative();
            case STANDARD -> ValidationThresholds.defaults();
            case AGGRESSIVE -> aggressive();
            case CONCENTRATION_REVIEW -> concentrationReview();
        };
    }

    private static ValidationThresholds conservative() {
        return new ValidationThresholds(
                0.84,   // passThreshold
                0.64,   // watchThreshold
                0.92,   // concentrationThreshold
                0.84,   // standardDeploymentThreshold
                0.64,   // probeDeploymentThreshold
                0.64,   // observeDeploymentThreshold
                0.85,   // concentrationDeploymentConfidenceThreshold
                0.84,   // concentrationAsymmetryThreshold
                0.84,   // concentrationEarlynessThreshold
                0.70,   // standardDeploymentConfidenceThreshold
                0.50,   // probeDeploymentConfidenceThreshold
                0.72,
                0.62,
                0.55,
                0.55,
                0.65,
                0.50,
                0.18,
                0.14,
                0.16,
                0.12,
                0.10,
                0.18,
                0.07,
                0.05
        );
    }

    private static ValidationThresholds aggressive() {
        return new ValidationThresholds(
                0.72,   // passThreshold
                0.50,   // watchThreshold
                0.84,   // concentrationThreshold
                0.72,   // standardDeploymentThreshold
                0.50,   // probeDeploymentThreshold
                0.50,   // observeDeploymentThreshold
                0.75,   // concentrationDeploymentConfidenceThreshold
                0.72,   // concentrationAsymmetryThreshold
                0.72,   // concentrationEarlynessThreshold
                0.60,   // standardDeploymentConfidenceThreshold
                0.40,   // probeDeploymentConfidenceThreshold
                0.58,
                0.48,
                0.38,
                0.38,
                0.48,
                0.32,
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

    private static ValidationThresholds concentrationReview() {
        return new ValidationThresholds(
                0.88,   // passThreshold
                0.66,   // watchThreshold
                0.93,   // concentrationThreshold
                0.88,   // standardDeploymentThreshold
                0.66,   // probeDeploymentThreshold
                0.66,   // observeDeploymentThreshold
                0.88,   // concentrationDeploymentConfidenceThreshold
                0.88,   // concentrationAsymmetryThreshold
                0.88,   // concentrationEarlynessThreshold
                0.75,   // standardDeploymentConfidenceThreshold
                0.55,   // probeDeploymentConfidenceThreshold
                0.75,
                0.68,
                0.60,
                0.60,
                0.72,
                0.55,
                0.20,
                0.14,
                0.16,
                0.12,
                0.10,
                0.18,
                0.06,
                0.04
        );
    }
}
