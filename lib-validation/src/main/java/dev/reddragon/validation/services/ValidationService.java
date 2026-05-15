package dev.reddragon.validation.services;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.services.engine.DisequilibriumValidationEngine;
import dev.reddragon.validation.services.engine.RiskFlagResolver;
import dev.reddragon.validation.services.engine.ValidationConfidenceScorer;
import dev.reddragon.validation.models.CandidateValidationInput;
import dev.reddragon.validation.models.RiskFlag;
import dev.reddragon.validation.models.ValidationAudit;
import dev.reddragon.validation.models.ValidationResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Stable facade for the validation subsystem.
 *
 * <p>The no-arg constructor uses default thresholds. Pass explicit
 * {@link ValidationThresholds} to apply a named profile preset.
 */
public class ValidationService {

    private final DisequilibriumValidationEngine validationEngine;
    private final RiskFlagResolver riskFlagResolver;
    private final ValidationConfidenceScorer confidenceScorer;

    /** Creates a service using the default (STANDARD) threshold profile. */
    public ValidationService() {
        this(ValidationThresholds.defaults());
    }

    /** Creates a service using the supplied thresholds — use with {@link dev.reddragon.validation.config.ValidationThresholdProfileFactory}. */
    public ValidationService(ValidationThresholds thresholds) {
        Objects.requireNonNull(thresholds, "thresholds is required");
        this.validationEngine = new DisequilibriumValidationEngine(thresholds);
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
