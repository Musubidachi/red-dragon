package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.models.CandidateValidationInput;
import dev.reddragon.validation.models.ReasonCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates validation rules that should block review before score aggregation.
 */
public class HardGateEvaluator {

    private final ValidationThresholds thresholds;

    public HardGateEvaluator(ValidationThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds is required");
    }

    /**
     * Main processing flow.
     */
    public List<ReasonCode> process(CandidateValidationInput input) {
        Objects.requireNonNull(input, "input is required");

        List<ReasonCode> failures = new ArrayList<>();

        addRequiredDataFailure(input, failures);
        addCatalystFailure(input, failures);
        addStructuralRealityFailure(input, failures);
        addMaterialityFailure(input, failures);
        addEarlynessFailure(input, failures);
        addEquilibriumFailure(input, failures);
        addAsymmetryFailure(input, failures);
        addRegimeFailure(input, failures);
        addHardGateMarker(failures);

        return List.copyOf(failures);
    }

    private void addRequiredDataFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (!input.requiredDataPresent()) {
            failures.add(ReasonCode.REQUIRED_DATA_MISSING);
        }
    }

    private void addCatalystFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (!input.credibleCatalyst()) {
            failures.add(ReasonCode.CATALYST_NOT_CREDIBLE);
        }
    }

    private void addStructuralRealityFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.structuralRealityScore() < thresholds.minStructuralReality()) {
            failures.add(ReasonCode.STRUCTURAL_CATALYST_WEAK);
        }
    }

    private void addMaterialityFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.materialSignificanceScore() < thresholds.minMaterialSignificance()) {
            failures.add(ReasonCode.MATERIAL_IMPACT_INSUFFICIENT);
        }
    }

    private void addEarlynessFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.earlynessScore() < thresholds.minEarlyness()) {
            failures.add(ReasonCode.MAINSTREAM_SATURATION);
        }

        if (input.euphoricOrSaturated()) {
            failures.add(ReasonCode.EUPHORIC_REFLEXIVITY);
        }
    }

    private void addEquilibriumFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.equilibriumQualityScore() < thresholds.minEquilibriumQuality()) {
            failures.add(ReasonCode.EQUILIBRIUM_DIRECTIONAL_HOSTILE);
        }

        if (input.hostileMarketStructure()) {
            failures.add(ReasonCode.EQUILIBRIUM_LIQUIDITY_DEGRADED);
        }
    }

    private void addAsymmetryFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.asymmetryScore() < thresholds.minAsymmetry()) {
            failures.add(ReasonCode.ASYMMETRY_UNFAVORABLE);
        }

        if (input.equilibriumAlreadyRepriced()) {
            failures.add(ReasonCode.ASYMMETRY_COMPRESSED);
        }
    }

    private void addRegimeFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.regimeCompatibilityScore() < thresholds.minRegimeCompatibility()) {
            failures.add(ReasonCode.REGIME_HOSTILE);
        }
    }

    private void addHardGateMarker(List<ReasonCode> failures) {
        if (!failures.isEmpty()) {
            failures.add(ReasonCode.HARD_GATE_FAILED);
        }
    }
}
