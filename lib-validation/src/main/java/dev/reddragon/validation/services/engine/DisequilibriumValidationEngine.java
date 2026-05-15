package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.models.CandidateValidationInput;
import dev.reddragon.validation.models.DeploymentTier;
import dev.reddragon.validation.models.ReasonCode;
import dev.reddragon.validation.models.ValidationFactor;
import dev.reddragon.validation.models.ValidationResult;
import dev.reddragon.validation.models.VerdictDecision;
import dev.reddragon.validation.utilities.ValidationScoreUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Main validation orchestration pipeline.
 */
public class DisequilibriumValidationEngine {

    private final ValidationThresholds thresholds;
    private final ValidationFactorFactory validationFactorFactory;
    private final HardGateEvaluator hardGateEvaluator;
    private final VerdictResolver verdictResolver;
    private final DeploymentResolver deploymentResolver;

    public DisequilibriumValidationEngine() {
        this(ValidationThresholds.defaults());
    }

    public DisequilibriumValidationEngine(ValidationThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds is required");
        this.validationFactorFactory = new ValidationFactorFactory(thresholds);
        this.hardGateEvaluator = new HardGateEvaluator(thresholds);
        this.verdictResolver = new VerdictResolver(thresholds);
        this.deploymentResolver = new DeploymentResolver(thresholds);
    }

    /**
     * Main processing flow.
     */
    public ValidationResult process(CandidateValidationInput input) {
        Objects.requireNonNull(input, "input is required");

        List<ValidationFactor> factors = validationFactorFactory.process(input);
        double score = score(factors);

        List<ReasonCode> reasons = reasons(factors);
        List<String> explanations = explanations(factors);

        List<ReasonCode> hardGateFailures = hardGateEvaluator.process(input);
        VerdictDecision verdictDecision = verdictResolver.process(score, hardGateFailures);

        reasons.addAll(verdictDecision.reasonCodes());
        explanations.addAll(verdictDecision.explanations());

        DeploymentTier deploymentTier = deploymentResolver.process(
                verdictDecision.verdict(),
                score,
                input
        );

        reasons.add(reasonForDeployment(deploymentTier));

        return new ValidationResult(
                input.candidateId(),
                input.symbol(),
                verdictDecision.verdict(),
                deploymentTier,
                score,
                factors,
                dedupeReasons(reasons),
                dedupeExplanations(explanations)
        );
    }

    private double score(List<ValidationFactor> factors) {
        double weightedScore = 0.0;
        double totalWeight = 0.0;

        for (ValidationFactor factor : factors) {
            weightedScore += factor.weightedScore();
            totalWeight += factor.weight();
        }

        return ValidationScoreUtils.weightedAverage(weightedScore, totalWeight);
    }

    private List<ReasonCode> reasons(List<ValidationFactor> factors) {
        List<ReasonCode> reasons = new ArrayList<>();

        for (ValidationFactor factor : factors) {
            reasons.add(factor.reasonCode());
        }

        return reasons;
    }

    private List<String> explanations(List<ValidationFactor> factors) {
        List<String> explanations = new ArrayList<>();

        for (ValidationFactor factor : factors) {
            if (!factor.explanation().isBlank()) {
                explanations.add(factor.explanation());
            }
        }

        return explanations;
    }

    private ReasonCode reasonForDeployment(DeploymentTier deploymentTier) {
        return switch (deploymentTier) {
            case CONCENTRATED -> ReasonCode.DEPLOYMENT_CONCENTRATION_CANDIDATE;
            case STANDARD -> ReasonCode.DEPLOYMENT_STANDARD_REVIEW;
            case PROBE -> ReasonCode.DEPLOYMENT_PROBE_ONLY;
            case OBSERVE -> ReasonCode.DEPLOYMENT_OBSERVE_ONLY;
            case NONE -> ReasonCode.DEPLOYMENT_REJECTED;
        };
    }

    private List<ReasonCode> dedupeReasons(List<ReasonCode> reasons) {
        Set<ReasonCode> deduped = new LinkedHashSet<>(reasons);
        return List.copyOf(deduped);
    }

    private List<String> dedupeExplanations(List<String> explanations) {
        Set<String> deduped = new LinkedHashSet<>(explanations);
        return List.copyOf(deduped);
    }
}
