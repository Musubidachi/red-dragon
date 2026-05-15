package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.models.CandidateValidationInput;
import dev.reddragon.validation.models.DeploymentTier;
import dev.reddragon.validation.models.Verdict;

import java.util.Objects;

/**
 * Resolves deployment posture from verdict and score quality.
 */
public class DeploymentResolver {

    private final ValidationThresholds thresholds;

    public DeploymentResolver(ValidationThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds is required");
    }

    /**
     * Main processing flow.
     */
    public DeploymentTier process(
            Verdict verdict,
            double score,
            CandidateValidationInput input
    ) {
        Objects.requireNonNull(verdict, "verdict is required");
        Objects.requireNonNull(input, "input is required");

        if (verdict == Verdict.REJECT) {
            return DeploymentTier.NONE;
        }

        if (concentrated(score, input)) {
            return DeploymentTier.CONCENTRATED;
        }

        if (standard(verdict, score)) {
            return DeploymentTier.STANDARD;
        }

        if (probe(score)) {
            return DeploymentTier.PROBE;
        }

        return DeploymentTier.OBSERVE;
    }

    private boolean concentrated(
            double score,
            CandidateValidationInput input
    ) {
        return score >= thresholds.concentrationThreshold()
                && input.deploymentConfidenceScore() >= thresholds.standardDeploymentThreshold()
                && input.asymmetryScore() >= thresholds.passThreshold()
                && input.earlynessScore() >= thresholds.passThreshold();
    }

    private boolean standard(Verdict verdict, double score) {
        return verdict == Verdict.PASS
                && score >= thresholds.standardDeploymentThreshold();
    }

    private boolean probe(double score) {
        return score >= thresholds.probeDeploymentThreshold();
    }
}
