package dev.reddragon.validation.service;

import dev.reddragon.validation.engine.DisequilibriumValidationEngine;
import dev.reddragon.validation.engine.RiskFlagResolver;
import dev.reddragon.validation.engine.ValidationConfidenceScorer;
import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.RiskFlag;
import dev.reddragon.validation.model.ValidationAudit;
import dev.reddragon.validation.model.ValidationResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Stable facade for the validation subsystem.
 */
public class ValidationService {

    private final DisequilibriumValidationEngine validationEngine;
    private final RiskFlagResolver riskFlagResolver;
    private final ValidationConfidenceScorer confidenceScorer;

    public ValidationService() {
        this.validationEngine = new DisequilibriumValidationEngine();
        this.riskFlagResolver = new RiskFlagResolver();
        this.confidenceScorer = new ValidationConfidenceScorer();
    }

    /**
     * Main processing flow.
     */
    public ValidationAudit process(CandidateValidationInput input) {
        Objects.requireNonNull(input, "input is required");

        ValidationResult validationResult = validationEngine.process(input);
        List<RiskFlag> riskFlags = riskFlagResolver.process(input);
        double confidenceScore = confidenceScorer.process(input, riskFlags);

        return new ValidationAudit(
                input.candidateId(),
                input.symbol(),
                Instant.now(),
                validationResult,
                riskFlags,
                confidenceScore
        );
    }
}
