package dev.reddragon.validation.config;

/**
 * Named threshold profiles for different review styles.
 *
 * <p>Each profile maps to a preset {@link ValidationThresholds} instance
 * via {@link ValidationThresholdProfileFactory}. The {@code STANDARD} profile
 * is the default and can be tuned with {@link ValidationThresholdProperties}.
 */
public enum ValidationProfile {
    CONSERVATIVE,
    STANDARD,
    AGGRESSIVE,
    CONCENTRATION_REVIEW;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case CONSERVATIVE        -> "Conservative";
            case STANDARD            -> "Standard";
            case AGGRESSIVE          -> "Aggressive";
            case CONCENTRATION_REVIEW -> "Concentration Review";
        };
    }
}
