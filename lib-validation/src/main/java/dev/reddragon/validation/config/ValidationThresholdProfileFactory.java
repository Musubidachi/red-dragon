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
                0.84,
                0.64,
                0.92,
                0.84,
                0.64,
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
                0.72,
                0.50,
                0.84,
                0.72,
                0.50,
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
                0.88,
                0.66,
                0.93,
                0.88,
                0.66,
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
