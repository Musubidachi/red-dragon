package dev.reddragon.domain.models;

/**
 * Describes long-horizon drift severity detected by
 * {@link dev.reddragon.analytics.services.meta.LongHorizonCalibrationAnalyzer}.
 */
public enum CalibrationDriftLevel {
    STABLE,
    MINOR_DRIFT,
    MODERATE_DRIFT,
    MAJOR_DRIFT;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case STABLE         -> "Stable";
            case MINOR_DRIFT    -> "Minor Drift";
            case MODERATE_DRIFT -> "Moderate Drift";
            case MAJOR_DRIFT    -> "Major Drift";
        };
    }

    /**
     * Returns {@code true} if this drift level warrants threshold recalibration
     * (MODERATE_DRIFT or MAJOR_DRIFT).
     */
    public boolean requiresAction() {
        return this == MODERATE_DRIFT || this == MAJOR_DRIFT;
    }
}
