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
        CandidateValidationInput input = baseInput()
                .candidateId("cand-001")
                .symbol("ASTS")
                .structuralRealityScore(0.94)
                .materialSignificanceScore(0.88)
                .earlynessScore(0.91)
                .equilibriumQualityScore(0.82)
                .reflexivityPotentialScore(0.84)
                .asymmetryScore(0.90)
                .regimeCompatibilityScore(0.76)
                .deploymentConfidenceScore(0.87)
                .notes("Government or contract-style catalyst with early propagation.")
                .build();

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
        CandidateValidationInput input = baseInput()
                .candidateId("cand-002")
                .symbol("HYPE")
                .structuralRealityScore(0.82)
                .materialSignificanceScore(0.80)
                .earlynessScore(0.85)
                .equilibriumQualityScore(0.78)
                .reflexivityPotentialScore(0.80)
                .asymmetryScore(0.81)
                .regimeCompatibilityScore(0.70)
                .deploymentConfidenceScore(0.75)
                .credibleCatalyst(false)
                .notes("High social excitement without credible structural evidence.")
                .build();

        ValidationResult result = engine.validate(input);

        assertEquals(Verdict.REJECT, result.verdict());
        assertEquals(DeploymentTier.NONE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.CATALYST_NOT_CREDIBLE));
        assertTrue(result.reasonCodes().contains(ReasonCode.HARD_GATE_FAILED));
    }

    @Test
    void rejectsCandidateWhenNarrativeIsEuphoricOrSaturated() {
        CandidateValidationInput input = baseInput()
                .candidateId("cand-003")
                .symbol("LATE")
                .structuralRealityScore(0.90)
                .materialSignificanceScore(0.86)
                .earlynessScore(0.72)
                .equilibriumQualityScore(0.76)
                .reflexivityPotentialScore(0.85)
                .asymmetryScore(0.79)
                .regimeCompatibilityScore(0.69)
                .deploymentConfidenceScore(0.72)
                .euphoricOrSaturated(true)
                .notes("Real catalyst, but market/social propagation appears fully saturated.")
                .build();

        ValidationResult result = engine.validate(input);

        assertEquals(Verdict.REJECT, result.verdict());
        assertEquals(DeploymentTier.NONE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.EUPHORIC_REFLEXIVITY));
        assertTrue(result.reasonCodes().contains(ReasonCode.REFLEXIVITY_OVEREXTENDED));
    }

    @Test
    void classifiesPlausibleButIncompleteDisequilibriumAsWatchProbe() {
        CandidateValidationInput input = baseInput()
                .candidateId("cand-004")
                .symbol("WATCH")
                .structuralRealityScore(0.72)
                .materialSignificanceScore(0.61)
                .earlynessScore(0.66)
                .equilibriumQualityScore(0.55)
                .reflexivityPotentialScore(0.52)
                .asymmetryScore(0.60)
                .regimeCompatibilityScore(0.50)
                .deploymentConfidenceScore(0.54)
                .notes("Real and somewhat early, but reflexivity and deployment confidence are not strong yet.")
                .build();

        ValidationResult result = engine.validate(input);

        assertEquals(Verdict.WATCH, result.verdict());
        assertEquals(DeploymentTier.PROBE, result.deploymentTier());
        assertTrue(result.reasonCodes().contains(ReasonCode.EARLY_EMERGING_PROPAGATION));
        assertTrue(result.reasonCodes().contains(ReasonCode.DEPLOYMENT_PROBE_ONLY));
    }

    private CandidateValidationInput.CandidateValidationInputBuilder baseInput() {
        return CandidateValidationInput.builder()
                .candidateId("candidate")
                .symbol("TICKER")
                .structuralRealityScore(0.70)
                .materialSignificanceScore(0.60)
                .earlynessScore(0.60)
                .equilibriumQualityScore(0.60)
                .reflexivityPotentialScore(0.50)
                .asymmetryScore(0.60)
                .regimeCompatibilityScore(0.50)
                .deploymentConfidenceScore(0.50)
                .credibleCatalyst(true)
                .requiredDataPresent(true)
                .euphoricOrSaturated(false)
                .hostileMarketStructure(false)
                .equilibriumAlreadyRepriced(false);
    }
}
