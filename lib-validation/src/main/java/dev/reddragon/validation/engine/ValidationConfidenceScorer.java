package dev.reddragon.validation.engine;

import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.RiskFlag;

import java.util.List;
import java.util.Objects;

/**
 * Scores confidence in the validation process itself.
 */
public class ValidationConfidenceScorer {

    /**
     * Main processing flow.
     */
    public double process(
            CandidateValidationInput input,
            List<RiskFlag> riskFlags
    ) {
        Objects.requireNonNull(input, "input is required");
        Objects.requireNonNull(riskFlags, "riskFlags is required");

        double score = 1.0;

        if (!input.requiredDataPresent()) {
            score -= 0.35;
        }

        if (riskFlags.contains(RiskFlag.VOLATILITY_RISK)) {
            score -= 0.15;
        }

        if (riskFlags.contains(RiskFlag.LIQUIDITY_RISK)) {
            score -= 0.15;
        }

        if (riskFlags.contains(RiskFlag.REFLEXIVITY_SATURATION_RISK)) {
            score -= 0.10;
        }

        if (riskFlags.contains(RiskFlag.ASYMMETRY_COMPRESSION_RISK)) {
            score -= 0.10;
        }

        return Math.max(0.0, Math.min(1.0, score));
    }
}
