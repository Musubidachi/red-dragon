package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.models.CandidateValidationInput;
import dev.reddragon.validation.models.ReasonCode;
import dev.reddragon.validation.models.ValidationFactor;
import dev.reddragon.validation.models.ValidationStage;

import java.util.List;
import java.util.Objects;

/**
 * Builds normalized validation factors from candidate inputs.
 */
public class ValidationFactorFactory {

    private final ValidationThresholds thresholds;

    public ValidationFactorFactory(ValidationThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds is required");
    }

    /**
     * Main processing flow.
     */
    public List<ValidationFactor> process(CandidateValidationInput input) {
        Objects.requireNonNull(input, "input is required");

        return List.of(
                structuralRealityFactor(input),
                materialSignificanceFactor(input),
                earlynessFactor(input),
                equilibriumFactor(input),
                reflexivityFactor(input),
                asymmetryFactor(input),
                regimeFactor(input),
                deploymentFactor(input)
        );
    }

    private ValidationFactor structuralRealityFactor(CandidateValidationInput input) {
        return new ValidationFactor(
                ValidationStage.STRUCTURAL_REALITY,
                input.structuralRealityScore(),
                thresholds.structuralRealityWeight(),
                structuralReason(input),
                structuralExplanation(input)
        );
    }

    private ValidationFactor materialSignificanceFactor(CandidateValidationInput input) {
        return new ValidationFactor(
                ValidationStage.MATERIAL_SIGNIFICANCE,
                input.materialSignificanceScore(),
                thresholds.materialSignificanceWeight(),
                materialReason(input.materialSignificanceScore()),
                materialExplanation(input.materialSignificanceScore())
        );
    }

    private ValidationFactor earlynessFactor(CandidateValidationInput input) {
        return new ValidationFactor(
                ValidationStage.EARLYNESS,
                input.earlynessScore(),
                thresholds.earlynessWeight(),
                earlynessReason(input),
                earlynessExplanation(input)
        );
    }

    private ValidationFactor equilibriumFactor(CandidateValidationInput input) {
        return new ValidationFactor(
                ValidationStage.EQUILIBRIUM_QUALITY,
                input.equilibriumQualityScore(),
                thresholds.equilibriumQualityWeight(),
                equilibriumReason(input),
                equilibriumExplanation(input)
        );
    }

    private ValidationFactor reflexivityFactor(CandidateValidationInput input) {
        return new ValidationFactor(
                ValidationStage.REFLEXIVITY_POTENTIAL,
                input.reflexivityPotentialScore(),
                thresholds.reflexivityPotentialWeight(),
                reflexivityReason(input),
                reflexivityExplanation(input)
        );
    }

    private ValidationFactor asymmetryFactor(CandidateValidationInput input) {
        return new ValidationFactor(
                ValidationStage.ASYMMETRY_QUALITY,
                input.asymmetryScore(),
                thresholds.asymmetryWeight(),
                asymmetryReason(input),
                asymmetryExplanation(input)
        );
    }

    private ValidationFactor regimeFactor(CandidateValidationInput input) {
        return new ValidationFactor(
                ValidationStage.REGIME_COMPATIBILITY,
                input.regimeCompatibilityScore(),
                thresholds.regimeCompatibilityWeight(),
                regimeReason(input.regimeCompatibilityScore()),
                regimeExplanation(input.regimeCompatibilityScore())
        );
    }

    private ValidationFactor deploymentFactor(CandidateValidationInput input) {
        return new ValidationFactor(
                ValidationStage.DEPLOYMENT_CONFIDENCE,
                input.deploymentConfidenceScore(),
                thresholds.deploymentConfidenceWeight(),
                deploymentReason(input.deploymentConfidenceScore()),
                deploymentExplanation(input.deploymentConfidenceScore())
        );
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
        if (input.credibleCatalyst()) {
            return "Structural reality: catalyst appears objective enough to evaluate.";
        }

        return "Structural reality: catalyst is not credible enough; likely hype or unsupported narrative.";
    }

    private ReasonCode materialReason(double score) {
        if (score >= 0.75) {
            return ReasonCode.MATERIAL_IMPACT_HIGH;
        }

        if (score >= 0.55) {
            return ReasonCode.MATERIAL_IMPACT_MEDIUM;
        }

        if (score >= 0.35) {
            return ReasonCode.MATERIAL_IMPACT_LOW;
        }

        return ReasonCode.MATERIAL_IMPACT_INSUFFICIENT;
    }

    private String materialExplanation(double score) {
        if (score >= 0.75) {
            return "Materiality: catalyst appears large enough to alter capital-flow expectations.";
        }

        if (score >= 0.55) {
            return "Materiality: catalyst is meaningful but not overwhelming.";
        }

        if (score >= 0.35) {
            return "Materiality: catalyst may matter, but impact appears limited.";
        }

        return "Materiality: catalyst is too small or vague to support the thesis.";
    }

    private ReasonCode earlynessReason(CandidateValidationInput input) {
        if (input.euphoricOrSaturated()) {
            return ReasonCode.EUPHORIC_REFLEXIVITY;
        }

        if (input.earlynessScore() >= 0.80) {
            return ReasonCode.EARLY_UNKNOWN_BUT_REAL;
        }

        if (input.earlynessScore() >= 0.60) {
            return ReasonCode.EARLY_EMERGING_PROPAGATION;
        }

        if (input.earlynessScore() >= 0.40) {
            return ReasonCode.SOCIAL_ACCELERATION_CAUTION;
        }

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
        if (input.hostileMarketStructure()) {
            return ReasonCode.EQUILIBRIUM_LIQUIDITY_DEGRADED;
        }

        if (input.equilibriumQualityScore() >= 0.75) {
            return ReasonCode.EQUILIBRIUM_RESTORATION_LIKELY;
        }

        if (input.equilibriumQualityScore() >= 0.50) {
            return ReasonCode.EQUILIBRIUM_ROTATIONAL_SUPPORTIVE;
        }

        if (input.equilibriumQualityScore() >= 0.35) {
            return ReasonCode.EQUILIBRIUM_UNSTABLE_VOLATILITY;
        }

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
        if (input.euphoricOrSaturated()) {
            return ReasonCode.REFLEXIVITY_OVEREXTENDED;
        }

        if (input.reflexivityPotentialScore() >= 0.65) {
            return ReasonCode.REFLEXIVITY_FORMING;
        }

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
        if (input.equilibriumAlreadyRepriced()) {
            return ReasonCode.ASYMMETRY_COMPRESSED;
        }

        if (input.asymmetryScore() >= 0.70) {
            return ReasonCode.ASYMMETRY_FAVORABLE;
        }

        if (input.asymmetryScore() >= 0.45) {
            return ReasonCode.ASYMMETRY_COMPRESSED;
        }

        return ReasonCode.ASYMMETRY_UNFAVORABLE;
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
        if (score >= 0.65) {
            return ReasonCode.REGIME_SUPPORTIVE;
        }

        if (score >= 0.40) {
            return ReasonCode.REGIME_NEUTRAL;
        }

        return ReasonCode.REGIME_HOSTILE;
    }

    private String regimeExplanation(double score) {
        if (score >= 0.65) {
            return "Regime: broader market conditions support this validation framework.";
        }

        if (score >= 0.40) {
            return "Regime: broader market conditions are mixed but not disqualifying.";
        }

        return "Regime: broader market state is hostile to the framework.";
    }

    private ReasonCode deploymentReason(double score) {
        if (score >= 0.80) {
            return ReasonCode.DEPLOYMENT_CONCENTRATION_CANDIDATE;
        }

        if (score >= 0.65) {
            return ReasonCode.DEPLOYMENT_STANDARD_REVIEW;
        }

        if (score >= 0.45) {
            return ReasonCode.DEPLOYMENT_PROBE_ONLY;
        }

        return ReasonCode.DEPLOYMENT_OBSERVE_ONLY;
    }

    private String deploymentExplanation(double score) {
        if (score >= 0.80) {
            return "Deployment: setup may deserve concentrated review if all other gates pass.";
        }

        if (score >= 0.65) {
            return "Deployment: setup may deserve standard capital review.";
        }

        if (score >= 0.45) {
            return "Deployment: setup is probe-only until confirmation improves.";
        }

        return "Deployment: setup should remain observation-only.";
    }
}
