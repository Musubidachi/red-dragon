package dev.reddragon.validation.services.engine;

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
        CandidateValidationInput input = new CandidateValidationInput(
                "candidate",
                "ASTS",
                0.95,
                0.90,
                0.92,
                0.80,
                0.80,
                0.92,
                0.80,
                0.90,
                true,
                true,
                false,
                false,
                false,
                "excellent"
        );

        DeploymentTier tier = resolver.process(Verdict.PASS, 0.90, input);

        assertEquals(DeploymentTier.CONCENTRATED, tier);
    }
}
