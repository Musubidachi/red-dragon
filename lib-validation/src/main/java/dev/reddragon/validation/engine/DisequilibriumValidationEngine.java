package dev.reddragon.validation.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.DeploymentTier;
import dev.reddragon.validation.model.ReasonCode;
import dev.reddragon.validation.model.ValidationFactor;
import dev.reddragon.validation.model.ValidationResult;
import dev.reddragon.validation.model.ValidationStage;
import dev.reddragon.validation.model.Verdict;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic validation engine centered on one question:
 *
 * Has reality changed before equilibrium fully adapts?
 *
 * The engine is deliberately strict. It is meant to support a selective trading
 * style where capital is deployed only when the candidate is real, meaningful,
 * early enough, structurally supported, and still asymmetric.
 */
public class DisequilibriumValidationEngine {

    private final ValidationThresholds thresholds;

    public DisequilibriumValidationEngine() {
        this(ValidationThresholds.defaults());
    }

    public DisequilibriumValidationEngine(ValidationThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds is required");
    }

    public ValidationResult validate(CandidateValidationInput input) {
        Objects.requireNonNull(input, "input is required");

        List<ValidationFactor> factors = factors(input);
        double score = weightedScore(factors);

        List<ReasonCode> reasons = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        for (ValidationFactor factor : factors) {
            reasons.add(factor.reasonCode());
            if (!factor.explanation().isBlank()) {
                explanations.add(factor.explanation());
            }
        }

        List<ReasonCode> hardGateFailures = hardGateFailures(input);
        reasons.addAll(hardGateFailures);

        Verdict verdict;
        if (!hardGateFailures.isEmpty()) {
            verdict = Verdict.REJECT;
            explanations.add("Rejected because one or more hard gates failed before scoring could justify review.");
        } else if (score >= thresholds.passThreshold()) {
            verdict = Verdict.PASS;
            explanations.add("Passed: evidence supports a real, meaningful, early disequilibrium with favorable remaining asymmetry.");
        } else if (score >= thresholds.watchThreshold()) {
            verdict = Verdict.WATCH;
            explanations.add("Watch: candidate has a plausible disequilibrium, but confirmation or asymmetry is not strong enough for a pass.");
        } else {
            verdict = Verdict.REJECT;
            reasons.add(ReasonCode.SCORE_BELOW_THRESHOLD);
            explanations.add("Rejected because the aggregate validation score is below the watch threshold.");
        }

        DeploymentTier deploymentTier = deploymentTier(verdict, score, input);
        reasons.add(reasonForDeployment(deploymentTier));

        return new ValidationResult(
                input.candidateId(),
                input.symbol(),
                verdict,
                deploymentTier,
                score,
                factors,
                dedupe(reasons),
                dedupeStrings(explanations)
        );
    }

    private List<ValidationFactor> factors(CandidateValidationInput input) {
        return List.of(
                new ValidationFactor(
                        ValidationStage.STRUCTURAL_REALITY,
                        input.structuralRealityScore(),
                        thresholds.structuralRealityWeight(),
                        structuralReason(input),
                        structuralExplanation(input)
                ),
                new ValidationFactor(
                        ValidationStage.MATERIAL_SIGNIFICANCE,
                        input.materialSignificanceScore(),
                        thresholds.materialSignificanceWeight(),
                        materialReason(input.materialSignificanceScore()),
                        materialExplanation(input.materialSignificanceScore())
                ),
                new ValidationFactor(
                        ValidationStage.EARLYNESS,
                        input.earlynessScore(),
                        thresholds.earlynessWeight(),
                        earlynessReason(input),
                        earlynessExplanation(input)
                ),
                new ValidationFactor(
                        ValidationStage.EQUILIBRIUM_QUALITY,
                        input.equilibriumQualityScore(),
                        thresholds.equilibriumQualityWeight(),
                        equilibriumReason(input),
                        equilibriumExplanation(input)
                ),
                new ValidationFactor(
                        ValidationStage.REFLEXIVITY_POTENTIAL,
                        input.reflexivityPotentialScore(),
                        thresholds.reflexivityPotentialWeight(),
                        reflexivityReason(input),
                        reflexivityExplanation(input)
                ),
                new ValidationFactor(
                        ValidationStage.ASYMmetry_QUALITY,
                        input.asymmetryScore(),
                        thresholds.asymmetryWeight(),
                        asymmetryReason(input),
                        asymmetryExplanation(input)
                ),
                new ValidationFactor(
                        ValidationStage.REGIME_COMPATIBILITY,
                        input.regimeCompatibilityScore(),
                        thresholds.regimeCompatibilityWeight(),
                        regimeReason(input.regimeCompatibilityScore()),
                        regimeExplanation(input.regimeCompatibilityScore())
                ),
                new ValidationFactor(
                        ValidationStage.DEPLOYMENT_CONFIDENCE,
                        input.deploymentConfidenceScore(),
                        thresholds.deploymentConfidenceWeight(),
                        deploymentConfidenceReason(input.deploymentConfidenceScore()),
                        deploymentConfidenceExplanation(input.deploymentConfidenceScore())
                )
        );
    }

    private List<ReasonCode> hardGateFailures(CandidateValidationInput input) {
        List<ReasonCode> failures = new ArrayList<>();

        if (!input.requiredDataPresent()) {
            failures.add(ReasonCode.REQUIRED_DATA_MISSING);
        }
        if (!input.credibleCatalyst()) {
            failures.add(ReasonCode.CATALYST_NOT_CREDIBLE);
        }
        if (input.structuralRealityScore() < thresholds.minStructuralReality()) {
            failures.add(ReasonCode.STRUCTURAL_CATALYST_WEAK);
        }
        if (input.materialSignificanceScore() < thresholds.minMaterialSignificance()) {
            failures.add(ReasonCode.MATERIAL_IMPACT_INSUFFICIENT);
        }
        if (input.earlynessScore() < thresholds.minEarlyness()) {
            failures.add(ReasonCode.MAINSTREAM_SATURATION);
        }
        if (input.euphoricOrSaturated()) {
            failures.add(ReasonCode.EUPHORIC_REFLEXIVITY);
        }
        if (input.equilibriumQualityScore() < thresholds.minEquilibriumQuality()) {
            failures.add(ReasonCode.EQUILIBRIUM_DIRECTIONAL_HOSTILE);
        }
        if (input.hostileMarketStructure()) {
            failures.add(ReasonCode.EQUILIBRIUM_LIQUIDITY_DEGRADED);
        }
        if (input.asymmetryScore() < thresholds.minAsymmetry()) {
            failures.add(ReasonCode.ASYMmetry_UNFAVORABLE);
        }
        if (input.equilibriumAlreadyRepriced()) {
            failures.add(ReasonCode.ASYMmetry_COMPRESSED);
        }
        if (input.regimeCompatibilityScore() < thresholds.minRegimeCompatibility()) {
            failures.add(ReasonCode.REGIME_HOSTILE);
        }

        if (!failures.isEmpty()) {
            failures.add(ReasonCode.HARD_GATE_FAILED);
        }
        return failures;
    }

    private double weightedScore(List<ValidationFactor> factors) {
        double weighted = 0.0;
        double totalWeight = 0.0;
        for (ValidationFactor factor : factors) {
            weighted += factor.weightedScore();
            totalWeight += factor.weight();
        }
        return totalWeight == 0.0 ? 0.0 : weighted / totalWeight;
    }

    private DeploymentTier deploymentTier(Verdict verdict, double score, CandidateValidationInput input) {
        if (verdict == Verdict.REJECT) {
            return DeploymentTier.NONE;
        }
        if (score >= thresholds.concentrationThreshold()
                && input.deploymentConfidenceScore() >= thresholds.standardDeploymentThreshold()
                && input.asymmetryScore() >= thresholds.passThreshold()
                && input.earlynessScore() >= thresholds.passThreshold()) {
            return DeploymentTier.CONCENTRATED;
        }
        if (verdict == Verdict.PASS && score >= thresholds.standardDeploymentThreshold()) {
            return DeploymentTier.STANDARD;
        }
        if (score >= thresholds.probeDeploymentThreshold()) {
            return DeploymentTier.PROBE;
        }
        return DeploymentTier.OBSERVE;
    }

    private ReasonCode structuralReason(CandidateValidationInput input) {
        if (!input.credibleCatalyst()) {
            return ReasonCode.CATALYST_NOT_CREDIBLE;
        }
        if (input.structuralRealityScore() >= 0.75) {
            return ReasonCode.STRUCTURAL_CATALYST_CONFIRMED;
        }
        if (input.structuralRealityScore() >= 0.45) {
            return ReasonCode.STRUCTURAL_CATALYST_WEAK;
        }
        return ReasonCode.CATALYST_MISSING;
    }

    private String structuralExplanation(CandidateValidationInput input) {
        return input.credibleCatalyst()
                ? "Structural reality: catalyst appears objective enough to evaluate."
                : "Structural reality: catalyst is not credible enough; likely hype or unsupported narrative.";
    }

    private ReasonCode materialReason(double score) {
        if (score >= 0.75) return ReasonCode.MATERIAL_IMPACT_HIGH;
        if (score >= 0.55) return ReasonCode.MATERIAL_IMPACT_MEDIUM;
        if (score >= 0.35) return ReasonCode.MATERIAL_IMPACT_LOW;
        return ReasonCode.MATERIAL_IMPACT_INSUFFICIENT;
    }

    private String materialExplanation(double score) {
        if (score >= 0.75) return "Materiality: catalyst appears large enough to alter capital-flow expectations.";
        if (score >= 0.55) return "Materiality: catalyst is meaningful but not overwhelming.";
        if (score >= 0.35) return "Materiality: catalyst may matter, but impact appears limited.";
        return "Materiality: catalyst is too small or vague to support the thesis.";
    }

    private ReasonCode earlynessReason(CandidateValidationInput input) {
        if (input.euphoricOrSaturated()) return ReasonCode.EUPHORIC_REFLEXIVITY;
        if (input.earlynessScore() >= 0.80) return ReasonCode.EARLY_UNKNOWN_BUT_REAL;
        if (input.earlynessScore() >= 0.60) return ReasonCode.EARLY_EMERGING_PROPAGATION;
        if (input.earlynessScore() >= 0.40) return ReasonCode.SOCIAL_ACCELERATION_CAUTION;
        return ReasonCode.MAINSTREAM_SATURATION;
    }

    private String earlynessExplanation(CandidateValidationInput input) {
        if (input.euphoricOrSaturated()) {
            return "Earlyness: narrative appears saturated or euphoric; asymmetry is likely compressed.";
        }
        if (input.earlynessScore() >= 0.80) {
            return "Earlyness: catalyst appears real while broader market adaptation is still incomplete.";
        }
        if (input.earlynessScore() >= 0.60) {
            return "Earlyness: propagation appears to be emerging but not mature.";
        }
        if (input.earlynessScore() >= 0.40) {
            return "Earlyness: social acceleration is visible; proceed with caution.";
        }
        return "Earlyness: candidate appears too widely noticed or too late.";
    }

    private ReasonCode equilibriumReason(CandidateValidationInput input) {
        if (input.hostileMarketStructure()) return ReasonCode.EQUILIBRIUM_LIQUIDITY_DEGRADED;
        if (input.equilibriumQualityScore() >= 0.75) return ReasonCode.EQUILIBRIUM_RESTORATION_LIKELY;
        if (input.equilibriumQualityScore() >= 0.50) return ReasonCode.EQUILIBRIUM_ROTATIONAL_SUPPORTIVE;
        if (input.equilibriumQualityScore() >= 0.35) return ReasonCode.EQUILIBRIUM_UNSTABLE_VOLATILITY;
        return ReasonCode.EQUILIBRIUM_DIRECTIONAL_HOSTILE;
    }

    private String equilibriumExplanation(CandidateValidationInput input) {
        if (input.hostileMarketStructure()) {
            return "Equilibrium: liquidity, volatility, or spread structure is hostile.";
        }
        if (input.equilibriumQualityScore() >= 0.75) {
            return "Equilibrium: structure supports restoration or controlled expansion.";
        }
        if (input.equilibriumQualityScore() >= 0.50) {
            return "Equilibrium: market appears rotational enough to support the framework.";
        }
        if (input.equilibriumQualityScore() >= 0.35) {
            return "Equilibrium: volatility is unstable; restoration probability is degraded.";
        }
        return "Equilibrium: directional or disorderly behavior is hostile to the setup.";
    }

    private ReasonCode reflexivityReason(CandidateValidationInput input) {
        if (input.euphoricOrSaturated()) return ReasonCode.REFLEXIVITY_OVEREXTENDED;
        if (input.reflexivityPotentialScore() >= 0.65) return ReasonCode.REFLEXIVITY_FORMING;
        return ReasonCode.REFLEXIVITY_NOT_YET_VISIBLE;
    }

    private String reflexivityExplanation(CandidateValidationInput input) {
        if (input.euphoricOrSaturated()) {
            return "Reflexivity: propagation is overextended rather than forming.";
        }
        if (input.reflexivityPotentialScore() >= 0.65) {
            return "Reflexivity: conditions suggest structural reality may attract increasing participation.";
        }
        return "Reflexivity: propagation is not clearly visible yet.";
    }

    private ReasonCode asymmetryReason(CandidateValidationInput input) {
        if (input.equilibriumAlreadyRepriced()) return ReasonCode.ASYMmetry_COMPRESSED;
        if (input.asymmetryScore() >= 0.70) return ReasonCode.ASYMmetry_FAVORABLE;
        if (input.asymmetryScore() >= 0.45) return ReasonCode.ASYMmetry_COMPRESSED;
        return ReasonCode.ASYMmetry_UNFAVORABLE;
    }

    private String asymmetryExplanation(CandidateValidationInput input) {
        if (input.equilibriumAlreadyRepriced()) {
            return "Asymmetry: equilibrium appears already repriced; remaining payoff is compressed.";
        }
        if (input.asymmetryScore() >= 0.70) {
            return "Asymmetry: upside/downside distribution remains favorable.";
        }
        if (input.asymmetryScore() >= 0.45) {
            return "Asymmetry: payoff exists but is becoming compressed.";
        }
        return "Asymmetry: downside or late entry risk dominates.";
    }

    private ReasonCode regimeReason(double score) {
        if (score >= 0.65) return ReasonCode.REGIME_SUPPORTIVE;
        if (score >= 0.40) return ReasonCode.REGIME_NEUTRAL;
        return ReasonCode.REGIME_HOSTILE;
    }

    private String regimeExplanation(double score) {
        if (score >= 0.65) return "Regime: broader market conditions support this validation framework.";
        if (score >= 0.40) return "Regime: broader market conditions are mixed but not disqualifying.";
        return "Regime: broader market state is hostile to the framework.";
    }

    private ReasonCode deploymentConfidenceReason(double score) {
        if (score >= 0.80) return ReasonCode.DEPLOYMENT_CONCENTRATION_CANDIDATE;
        if (score >= 0.65) return ReasonCode.DEPLOYMENT_STANDARD_REVIEW;
        if (score >= 0.45) return ReasonCode.DEPLOYMENT_PROBE_ONLY;
        return ReasonCode.DEPLOYMENT_OBSERVE_ONLY;
    }

    private String deploymentConfidenceExplanation(double score) {
        if (score >= 0.80) return "Deployment: setup may deserve concentrated review if all other gates pass.";
        if (score >= 0.65) return "Deployment: setup may deserve standard capital review.";
        if (score >= 0.45) return "Deployment: setup is probe-only until confirmation improves.";
        return "Deployment: setup should remain observation-only.";
    }

    private ReasonCode reasonForDeployment(DeploymentTier tier) {
        return switch (tier) {
            case CONCENTRATED -> ReasonCode.DEPLOYMENT_CONCENTRATION_CANDIDATE;
            case STANDARD -> ReasonCode.DEPLOYMENT_STANDARD_REVIEW;
            case PROBE -> ReasonCode.DEPLOYMENT_PROBE_ONLY;
            case OBSERVE -> ReasonCode.DEPLOYMENT_OBSERVE_ONLY;
            case NONE -> ReasonCode.DEPLOYMENT_REJECTED;
        };
    }

    private List<ReasonCode> dedupe(List<ReasonCode> codes) {
        Set<ReasonCode> set = new LinkedHashSet<>(codes);
        return List.copyOf(set);
    }

    private List<String> dedupeStrings(List<String> values) {
        Set<String> set = new LinkedHashSet<>(values);
        return List.copyOf(set);
    }
}
