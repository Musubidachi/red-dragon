package dev.reddragon.validation.engine;

import dev.reddragon.validation.model.CandidateValidationInput;
import dev.reddragon.validation.model.DeploymentTier;
import dev.reddragon.validation.model.ReasonCode;
import dev.reddragon.validation.model.ValidationResult;
import dev.reddragon.validation.model.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisequilibriumValidationEngineTest {

    private final DisequilibriumValidationEngine engine = new DisequilibriumValidationEngine();

    @Test
    void validatesRealEarlyAsymmetricDisequilibriumAsPassWithConcentratedDeployment() {
        CandidateValidationInput input = new CandidateValidationInput(
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

        ValidationResult result = engine.validate(input);

        assertEquals(Verdict.PASS, result.verdict());
        assertEquals(DeploymentTier.CONCENTRATED, result.deploymentTier());
        assertTrue(result.score() >= 0.87);
        assertTrue(result.reasonCodes().contains(ReasonCode.STRUCTURAL_CATALYST_CONFIRMED));
        assertTrue(result.reasonCodes().contains(ReasonCode.ASYMMETRY_FAVORABLE));
        assertTrue(result.reasonCodes().contains(ReasonCode.DEPLOYMENT_CONCENTRATION_CANDIDATE));
    }

    @Test
    void rejectsCandidateWhenCatalystIsNotCredibleEvenIfOtherScoresLookGood() {
        CandidateValidationInput input = new CandidateValidationInput(
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

        ValidationResult result = engine.validate(input);

        assertEquals(Verdict.REJECT, result.verdict());
        assertEquals(DeploymentTier.NONE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.CATALYST_NOT_CREDIBLE));
        assertTrue(result.reasonCodes().contains(ReasonCode.HARD_GATE_FAILED));
    }

    @Test
    void rejectsCandidateWhenNarrativeIsEuphoricOrSaturated() {
        CandidateValidationInput input = new CandidateValidationInput(
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

        ValidationResult result = engine.validate(input);

        assertEquals(Verdict.REJECT, result.verdict());
        assertEquals(DeploymentTier.NONE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.EUPHORIC_REFLEXIVITY));
        assertTrue(result.reasonCodes().contains(ReasonCode.REFLEXIVITY_OVEREXTENDED));
    }

    @Test
    void classifiesPlausibleButIncompleteDisequilibriumAsWatchProbe() {
        CandidateValidationInput input = new CandidateValidationInput(
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

        ValidationResult result = engine.validate(input);

        assertEquals(Verdict.WATCH, result.verdict());
        assertEquals(DeploymentTier.PROBE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.EARLY_EMERGING_PROPAGATION));
        assertTrue(result.reasonCodes().contains(ReasonCode.DEPLOYMENT_PROBE_ONLY));
    }
}
