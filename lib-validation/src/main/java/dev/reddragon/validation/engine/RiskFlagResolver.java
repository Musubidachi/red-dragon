package dev.reddragon.validation.engine;

import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.RiskFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves qualitative validation risks from the normalized input.
 */
public class RiskFlagResolver {

    /**
     * Main processing flow.
     */
    public List<RiskFlag> process(CandidateValidationInput input) {
        Objects.requireNonNull(input, "input is required");

        List<RiskFlag> riskFlags = new ArrayList<>();

        addDataQualityRisk(input, riskFlags);
        addLateEntryRisk(input, riskFlags);
        addVolatilityRisk(input, riskFlags);
        addAsymmetryRisk(input, riskFlags);
        addRegimeRisk(input, riskFlags);
        addLiquidityRisk(input, riskFlags);
        addReflexivityRisk(input, riskFlags);

        return List.copyOf(riskFlags);
    }

    private void addDataQualityRisk(
            CandidateValidationInput input,
            List<RiskFlag> riskFlags
    ) {
        if (!input.requiredDataPresent()) {
            riskFlags.add(RiskFlag.DATA_QUALITY_RISK);
        }
    }

    private void addLateEntryRisk(
            CandidateValidationInput input,
            List<RiskFlag> riskFlags
    ) {
        if (input.earlynessScore() < 0.50) {
            riskFlags.add(RiskFlag.LATE_ENTRY_RISK);
        }
    }

    private void addVolatilityRisk(
            CandidateValidationInput input,
            List<RiskFlag> riskFlags
    ) {
        if (input.equilibriumQualityScore() < 0.45) {
            riskFlags.add(RiskFlag.VOLATILITY_RISK);
        }
    }

    private void addAsymmetryRisk(
            CandidateValidationInput input,
            List<RiskFlag> riskFlags
    ) {
        if (input.asymmetryScore() < 0.60 || input.equilibriumAlreadyRepriced()) {
            riskFlags.add(RiskFlag.ASYMMETRY_COMPRESSION_RISK);
        }
    }

    private void addRegimeRisk(
            CandidateValidationInput input,
            List<RiskFlag> riskFlags
    ) {
        if (input.regimeCompatibilityScore() < 0.50) {
            riskFlags.add(RiskFlag.REGIME_RISK);
        }
    }

    private void addLiquidityRisk(
            CandidateValidationInput input,
            List<RiskFlag> riskFlags
    ) {
        if (input.hostileMarketStructure()) {
            riskFlags.add(RiskFlag.LIQUIDITY_RISK);
        }
    }

    private void addReflexivityRisk(
            CandidateValidationInput input,
            List<RiskFlag> riskFlags
    ) {
        if (input.euphoricOrSaturated()) {
            riskFlags.add(RiskFlag.REFLEXIVITY_SATURATION_RISK);
        }
    }
}
