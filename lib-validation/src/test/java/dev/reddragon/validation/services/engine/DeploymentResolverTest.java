package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholdProperties;
import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.DeploymentTier;
import dev.reddragon.domain.models.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeploymentResolverTest {

    private final DeploymentResolver resolver = new DeploymentResolver(
            ValidationThresholds.defaults()
    );

    @Test
    void resolvesConcentratedDeploymentForExceptionalSetup() {
        CandidateValidationInput input = inputWithScores(0.92, 0.92, 0.90);

        DeploymentTier tier = resolver.process(Verdict.PASS, 0.90, input);

        assertEquals(DeploymentTier.CONCENTRATED, tier);
    }

    @Test
    void downgradesPassToProbeWhenDeploymentConfidenceMissesStandardGate() {
        CandidateValidationInput input = inputWithScores(0.90, 0.90, 0.50);

        DeploymentTier tier = resolver.process(Verdict.PASS, 0.82, input);

        assertEquals(DeploymentTier.PROBE, tier);
    }

    @Test
    void downgradesProbeScoreToObserveWhenDeploymentConfidenceMissesProbeGate() {
        CandidateValidationInput input = inputWithScores(0.90, 0.90, 0.40);

        DeploymentTier tier = resolver.process(Verdict.WATCH, 0.60, input);

        assertEquals(DeploymentTier.OBSERVE, tier);
    }

    @Test
    void concentrationInputThresholdsAreIndependentFromAggregatePassThreshold() {
        ValidationThresholdProperties properties = new ValidationThresholdProperties();
        properties.setPassThreshold(0.90);
        properties.setConcentrationThreshold(0.92);
        properties.setConcentrationAsymmetryThreshold(0.78);
        properties.setConcentrationEarlynessThreshold(0.78);
        DeploymentResolver strictResolver = new DeploymentResolver(properties.toThresholds());
        CandidateValidationInput input = inputWithScores(0.80, 0.80, 0.90);

        DeploymentTier tier = strictResolver.process(Verdict.PASS, 0.93, input);

        assertEquals(DeploymentTier.CONCENTRATED, tier);
    }

    private CandidateValidationInput inputWithScores(
            double earlynessScore,
            double asymmetryScore,
            double deploymentConfidenceScore
    ) {
        return new CandidateValidationInput(
                "candidate",
                "ASTS",
                0.95,
                0.90,
                earlynessScore,
                0.80,
                0.80,
                asymmetryScore,
                0.80,
                deploymentConfidenceScore,
                true,
                true,
                false,
                false,
                false,
                "excellent"
        );
    }
}
