package dev.reddragon.domain.models;

import dev.reddragon.math.ValidationScoreUtils;
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
        this.score = ValidationScoreUtils.requireNormalized("score", score);
        ValidationScoreUtils.requireNonNegative("weight", weight);
        this.weight = weight;
    }

    public double weightedScore() {
        return score * weight;
    }
}
