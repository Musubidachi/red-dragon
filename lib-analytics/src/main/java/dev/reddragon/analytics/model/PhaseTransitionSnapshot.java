package dev.reddragon.analytics.model;

import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Represents directional change in an analytics dimension.
 */
@Value
@Accessors(fluent = true)
public class PhaseTransitionSnapshot {
    double previousValue;
    double currentValue;
    double slope;
    double acceleration;

    public PhaseTransitionSnapshot(
            double previousValue,
            double currentValue,
            double slope,
            double acceleration
    ) {
        this.previousValue = previousValue;
        this.currentValue = currentValue;
        this.slope = slope;
        this.acceleration = acceleration;
    }

    public boolean improving() {
        return slope > 0.0;
    }

    public boolean deteriorating() {
        return slope < 0.0;
    }

    public boolean accelerating() {
        return acceleration > 0.0;
    }

    public boolean decelerating() {
        return acceleration < 0.0;
    }
}
