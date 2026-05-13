package dev.reddragon.validation.model;

/**
 * Machine-readable explanation codes for validation decisions.
 */
public enum ReasonCode {
    STRUCTURAL_CATALYST_CONFIRMED,
    STRUCTURAL_CATALYST_WEAK,
    CATALYST_NOT_CREDIBLE,
    CATALYST_MISSING,

    MATERIAL_IMPACT_HIGH,
    MATERIAL_IMPACT_MEDIUM,
    MATERIAL_IMPACT_LOW,
    MATERIAL_IMPACT_INSUFFICIENT,

    EARLY_UNKNOWN_BUT_REAL,
    EARLY_EMERGING_PROPAGATION,
    SOCIAL_ACCELERATION_CAUTION,
    MAINSTREAM_SATURATION,
    EUPHORIC_REFLEXIVITY,

    EQUILIBRIUM_RESTORATION_LIKELY,
    EQUILIBRIUM_ROTATIONAL_SUPPORTIVE,
    EQUILIBRIUM_DIRECTIONAL_HOSTILE,
    EQUILIBRIUM_UNSTABLE_VOLATILITY,
    EQUILIBRIUM_LIQUIDITY_DEGRADED,

    REFLEXIVITY_FORMING,
    REFLEXIVITY_NOT_YET_VISIBLE,
    REFLEXIVITY_OVEREXTENDED,

    ASYMMETRY_FAVORABLE,
    ASYMMETRY_COMPRESSED,
    ASYMMETRY_UNFAVORABLE,

    REGIME_SUPPORTIVE,
    REGIME_NEUTRAL,
    REGIME_HOSTILE,

    DEPLOYMENT_CONCENTRATION_CANDIDATE,
    DEPLOYMENT_STANDARD_REVIEW,
    DEPLOYMENT_PROBE_ONLY,
    DEPLOYMENT_OBSERVE_ONLY,
    DEPLOYMENT_REJECTED,

    REQUIRED_DATA_MISSING,
    SCORE_BELOW_THRESHOLD,
    HARD_GATE_FAILED;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        // Convert SCREAMING_SNAKE_CASE to Title Case With Spaces
        String raw = name().replace('_', ' ').toLowerCase();
        StringBuilder sb = new StringBuilder(raw.length());
        boolean capitalizeNext = true;
        for (char c : raw.toCharArray()) {
            sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
            capitalizeNext = (c == ' ');
        }
        return sb.toString();
    }

    /**
     * Returns {@code true} if this reason code represents a positive / supportive
     * signal rather than a warning, deficiency, or failure.
     */
    public boolean isPositive() {
        return switch (this) {
            case STRUCTURAL_CATALYST_CONFIRMED,
                 MATERIAL_IMPACT_HIGH,
                 MATERIAL_IMPACT_MEDIUM,
                 EARLY_UNKNOWN_BUT_REAL,
                 EARLY_EMERGING_PROPAGATION,
                 EQUILIBRIUM_RESTORATION_LIKELY,
                 EQUILIBRIUM_ROTATIONAL_SUPPORTIVE,
                 REFLEXIVITY_FORMING,
                 ASYMMETRY_FAVORABLE,
                 REGIME_SUPPORTIVE,
                 DEPLOYMENT_CONCENTRATION_CANDIDATE,
                 DEPLOYMENT_STANDARD_REVIEW -> true;
            default -> false;
        };
    }
}
