package dev.reddragon.domain.models;

/**
 * Suggested capital-deployment posture implied by validation strength.
 *
 * This does not size a trade or place an order. It expresses whether the
 * candidate deserves no action, observation, small probing, normal review,
 * or rare high-conviction concentration review.
 */
public enum DeploymentTier {
    NONE,
    OBSERVE,
    PROBE,
    STANDARD,
    CONCENTRATED;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case NONE        -> "None";
            case OBSERVE     -> "Observe";
            case PROBE       -> "Probe";
            case STANDARD    -> "Standard";
            case CONCENTRATED -> "Concentrated";
        };
    }

    /**
     * Returns {@code true} if this tier represents an actionable deployment posture
     * (i.e., at least PROBE — excludes NONE and OBSERVE).
     */
    public boolean isActionable() {
        return this == PROBE || this == STANDARD || this == CONCENTRATED;
    }
}
