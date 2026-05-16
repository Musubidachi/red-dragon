package dev.reddragon.domain.models;

import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Describes a contradiction or adversarial concern identified during analytics.
 */
@Value
@Accessors(fluent = true)
public class AdversarialFinding {
    AdversarialFindingType type;
    double severity;
    String explanation;

    public AdversarialFinding(
            AdversarialFindingType type,
            double severity,
            String explanation
    ) {
        this.type = Objects.requireNonNull(type, "type is required");
        this.severity = Math.max(0.0, Math.min(1.0, severity));
        this.explanation = explanation == null ? "" : explanation;
    }
}
