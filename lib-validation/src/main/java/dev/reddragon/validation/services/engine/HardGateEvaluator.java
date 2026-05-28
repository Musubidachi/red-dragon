package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.ReasonCode;

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

        boolean hardGateFailed = false;
        hardGateFailed |= addRequiredDataFailure(input, failures);
        hardGateFailed |= addCatalystFailure(input, failures);
        hardGateFailed |= addStructuralRealityFailure(input, failures);
        hardGateFailed |= addMaterialityFailure(input, failures);
        hardGateFailed |= addEarlynessFailure(input, failures);
        hardGateFailed |= addEquilibriumFailure(input, failures);
        hardGateFailed |= addAsymmetryFailure(input, failures);
        hardGateFailed |= addRegimeFailure(input, failures);
        addHardGateMarker(hardGateFailed, failures);

        return List.copyOf(failures);
    }

    private boolean addRequiredDataFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (!input.requiredDataPresent()) {
            failures.add(ReasonCode.REQUIRED_DATA_MISSING);
            return true;
        }

        return false;
    }

    private boolean addCatalystFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (!input.credibleCatalyst()) {
            failures.add(ReasonCode.CATALYST_NOT_CREDIBLE);
            return true;
        }

        return false;
    }

    private boolean addStructuralRealityFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.structuralRealityScore() < thresholds.minStructuralReality()) {
            failures.add(ReasonCode.STRUCTURAL_CATALYST_WEAK);
            return true;
        }

        return false;
    }

    private boolean addMaterialityFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.materialSignificanceScore() < thresholds.minMaterialSignificance()) {
            failures.add(ReasonCode.MATERIAL_IMPACT_INSUFFICIENT);
            return true;
        }

        return false;
    }

    private boolean addEarlynessFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        boolean failed = false;
        if (input.earlynessScore() < thresholds.minEarlyness()) {
            failures.add(ReasonCode.MAINSTREAM_SATURATION);
            failed = true;
        }

        if (input.euphoricOrSaturated()) {
            failures.add(ReasonCode.EUPHORIC_REFLEXIVITY);
            failed = true;
        }

        return failed;
    }

    private boolean addEquilibriumFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        boolean failed = false;
        if (input.equilibriumQualityScore() < thresholds.minEquilibriumQuality()) {
            failures.add(ReasonCode.EQUILIBRIUM_DIRECTIONAL_HOSTILE);
            failed = true;
        }

        if (input.hostileMarketStructure()) {
            failures.add(ReasonCode.EQUILIBRIUM_LIQUIDITY_DEGRADED);
            failed = true;
        }

        return failed;
    }

    private boolean addAsymmetryFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        boolean failed = false;
        if (input.asymmetryScore() < thresholds.minAsymmetry()) {
            failures.add(ReasonCode.ASYMMETRY_UNFAVORABLE);
            failed = true;
        }

        if (input.equilibriumAlreadyRepriced()) {
            failures.add(ReasonCode.ASYMMETRY_COMPRESSED);
            failed = true;
        }

        return failed;
    }

    private boolean addRegimeFailure(
            CandidateValidationInput input,
            List<ReasonCode> failures
    ) {
        if (input.regimeCompatibilityScore() < thresholds.minRegimeCompatibility()) {
            failures.add(ReasonCode.REGIME_HOSTILE);
            return true;
        }

        return false;
    }

    private void addHardGateMarker(boolean hardGateFailed, List<ReasonCode> failures) {
        if (hardGateFailed) {
            failures.add(ReasonCode.HARD_GATE_FAILED);
        }
    }
}
