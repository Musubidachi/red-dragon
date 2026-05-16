package dev.reddragon.validation.services.engine;

import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.DeploymentTier;
import dev.reddragon.domain.models.ReasonCode;
import dev.reddragon.domain.models.ValidationResult;
import dev.reddragon.domain.models.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisequilibriumValidationEngineTest {

    private final DisequilibriumValidationEngine engine = new DisequilibriumValidationEngine();

    @Test
    void validatesRealEarlyAsymmetricDisequilibriumAsPassWithConcentratedDeployment() {
        CandidateValidationInput input = input(
                "cand-001",
                "ASTS",
                0.94,
                0.88,
                0.91,
                0.82,
                0.84,
                0.90,
                0.76,
                0.87,
                true,
                true,
                false,
                false,
                false,
                "Government or contract-style catalyst with early propagation."
        );

        ValidationResult result = engine.process(input);

        assertEquals(Verdict.PASS, result.verdict());
        assertEquals(DeploymentTier.CONCENTRATED, result.deploymentTier());
        assertTrue(result.score() >= 0.87);
        assertTrue(result.reasonCodes().contains(ReasonCode.STRUCTURAL_CATALYST_CONFIRMED));
        assertTrue(result.reasonCodes().contains(ReasonCode.ASYMMETRY_FAVORABLE));
        assertTrue(result.reasonCodes().contains(ReasonCode.DEPLOYMENT_CONCENTRATION_CANDIDATE));
    }

    @Test
    void rejectsCandidateWhenCatalystIsNotCredibleEvenIfOtherScoresLookGood() {
        CandidateValidationInput input = input(
                "cand-002",
                "HYPE",
                0.82,
                0.80,
                0.85,
                0.78,
                0.80,
                0.81,
                0.70,
                0.75,
                false,
                true,
                false,
                false,
                false,
                "High social excitement without credible structural evidence."
        );

        ValidationResult result = engine.process(input);

        assertEquals(Verdict.REJECT, result.verdict());
        assertEquals(DeploymentTier.NONE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.CATALYST_NOT_CREDIBLE));
        assertTrue(result.reasonCodes().contains(ReasonCode.HARD_GATE_FAILED));
    }

    @Test
    void rejectsCandidateWhenNarrativeIsEuphoricOrSaturated() {
        CandidateValidationInput input = input(
                "cand-003",
                "LATE",
                0.90,
                0.86,
                0.72,
                0.76,
                0.85,
                0.79,
                0.69,
                0.72,
                true,
                true,
                true,
                false,
                false,
                "Real catalyst, but market/social propagation appears fully saturated."
        );

        ValidationResult result = engine.process(input);

        assertEquals(Verdict.REJECT, result.verdict());
        assertEquals(DeploymentTier.NONE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.EUPHORIC_REFLEXIVITY));
        assertTrue(result.reasonCodes().contains(ReasonCode.REFLEXIVITY_OVEREXTENDED));
    }

    @Test
    void classifiesPlausibleButIncompleteDisequilibriumAsWatchProbe() {
        CandidateValidationInput input = input(
                "cand-004",
                "WATCH",
                0.72,
                0.61,
                0.66,
                0.55,
                0.52,
                0.60,
                0.50,
                0.54,
                true,
                true,
                false,
                false,
                false,
                "Real and somewhat early, but reflexivity and deployment confidence are not strong yet."
        );

        ValidationResult result = engine.process(input);

        assertEquals(Verdict.WATCH, result.verdict());
        assertEquals(DeploymentTier.PROBE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.EARLY_EMERGING_PROPAGATION));
        assertTrue(result.reasonCodes().contains(ReasonCode.DEPLOYMENT_PROBE_ONLY));
    }

    private CandidateValidationInput input(
            String candidateId,
            String symbol,
            double structuralRealityScore,
            double materialSignificanceScore,
            double earlynessScore,
            double equilibriumQualityScore,
            double reflexivityPotentialScore,
            double asymmetryScore,
            double regimeCompatibilityScore,
            double deploymentConfidenceScore,
            boolean credibleCatalyst,
            boolean requiredDataPresent,
            boolean euphoricOrSaturated,
            boolean hostileMarketStructure,
            boolean equilibriumAlreadyRepriced,
            String notes
    ) {
        return new CandidateValidationInput(
                candidateId,
                symbol,
                structuralRealityScore,
                materialSignificanceScore,
                earlynessScore,
                equilibriumQualityScore,
                reflexivityPotentialScore,
                asymmetryScore,
                regimeCompatibilityScore,
                deploymentConfidenceScore,
                credibleCatalyst,
                requiredDataPresent,
                euphoricOrSaturated,
                hostileMarketStructure,
                equilibriumAlreadyRepriced,
                notes
        );
    }
}
