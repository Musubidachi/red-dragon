package dev.reddragon.analytics.propagation;

import dev.reddragon.analytics.model.PhaseLabel;
import dev.reddragon.analytics.model.PhaseTransitionSnapshot;

import java.util.Objects;

/**
 * Interprets propagation phase transitions.
 */
public class PropagationPhaseAnalyzer {

    /**
     * Main processing flow.
     */
    public PhaseLabel process(PhaseTransitionSnapshot propagation) {
        Objects.requireNonNull(propagation, "propagation is required");

        if (propagation.currentValue() < 0.30) {
            return PhaseLabel.UNKNOWN;
        }

        if (propagation.currentValue() < 0.50
                && propagation.improving()
                && propagation.accelerating()) {
            return PhaseLabel.EARLY_DISCOVERY;
        }

        if (propagation.currentValue() < 0.70
                && propagation.improving()) {
            return PhaseLabel.EMERGING_PROPAGATION;
        }

        if (propagation.currentValue() >= 0.70
                && propagation.accelerating()) {
            return PhaseLabel.EARLY_REFLEXIVITY;
        }

        if (propagation.currentValue() >= 0.85
                && propagation.accelerating()) {
            return PhaseLabel.BROAD_ACCELERATION;
        }

        if (propagation.currentValue() >= 0.85
                && propagation.decelerating()) {
            return PhaseLabel.LATE_REFLEXIVITY;
        }

        if (propagation.currentValue() >= 0.90
                && propagation.deteriorating()) {
            return PhaseLabel.SATURATION_RISK;
        }

        return PhaseLabel.UNKNOWN;
    }
}
