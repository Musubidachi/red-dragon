package dev.reddragon.analytics.services.classification;

import dev.reddragon.domain.models.PhaseLabel;
import dev.reddragon.domain.models.PhaseTransitionSnapshot;

import java.util.Objects;

/**
 * Interprets equilibrium quality transitions.
 */
public class EquilibriumPhaseAnalyzer {

    /**
     * Main processing flow.
     */
    public PhaseLabel process(PhaseTransitionSnapshot equilibrium) {
        Objects.requireNonNull(equilibrium, "equilibrium is required");

        if (equilibrium.currentValue() >= 0.70
                && equilibrium.improving()) {
            return PhaseLabel.EMERGING_PROPAGATION;
        }

        if (equilibrium.currentValue() >= 0.75
                && equilibrium.accelerating()) {
            return PhaseLabel.EARLY_REFLEXIVITY;
        }

        if (equilibrium.currentValue() < 0.45
                && equilibrium.deteriorating()) {
            return PhaseLabel.EQUILIBRIUM_BREAKDOWN;
        }

        if (equilibrium.currentValue() < 0.25) {
            return PhaseLabel.EXHAUSTION;
        }

        return PhaseLabel.UNKNOWN;
    }
}
