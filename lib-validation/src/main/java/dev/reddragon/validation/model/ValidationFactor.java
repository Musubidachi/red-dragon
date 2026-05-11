package dev.reddragon.validation.model;

import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * A scored validation dimension.
 *
 * Scores should be normalized from 0.0 to 1.0 where:
 * - 0.0 means invalid, hostile, saturated, or not supportive
 * - 0.5 means mixed / unclear
 * - 1.0 means highly supportive
 */
@Value
@Accessors(fluent = true)
public class ValidationFactor {
    ValidationStage stage;
    double score;
    double weight;
    ReasonCode reasonCode;
    String explanation;

    public ValidationFactor(
            ValidationStage stage,
            double score,
            double weight,
            ReasonCode reasonCode,
            String explanation
    ) {
        this.stage = Objects.requireNonNull(stage, "stage is required");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode is required");
        this.explanation = explanation == null ? "" : explanation;
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0");
        }
        if (weight < 0.0) {
            throw new IllegalArgumentException("weight must be non-negative");
        }
        this.score = score;
        this.weight = weight;
    }

    public double weightedScore() {
        return score * weight;
    }
}
