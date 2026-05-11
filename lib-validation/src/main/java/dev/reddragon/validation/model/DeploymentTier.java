package dev.reddragon.validation.model;

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
    CONCENTRATED
}
