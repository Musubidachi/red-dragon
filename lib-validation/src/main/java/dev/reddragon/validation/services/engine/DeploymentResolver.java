package dev.reddragon.validation.services.engine;

import dev.reddragon.math.ValidationScoreUtils;
import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.DeploymentTier;
import dev.reddragon.domain.models.Verdict;

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
        ValidationScoreUtils.requireNormalized("score", score);

        if (verdict == Verdict.REJECT) {
            return DeploymentTier.NONE;
        }

        if (concentrated(score, input)) {
            return DeploymentTier.CONCENTRATED;
        }

        if (standard(verdict, score, input)) {
            return DeploymentTier.STANDARD;
        }

        if (probe(score, input)) {
            return DeploymentTier.PROBE;
        }

        if (observe(score)) {
            return DeploymentTier.OBSERVE;
        }

        // Below the OBSERVE floor and not REJECT: fall back to NONE.
        return DeploymentTier.NONE;
    }

    private boolean concentrated(
            double score,
            CandidateValidationInput input
    ) {
        return score >= thresholds.concentrationThreshold()
                && input.deploymentConfidenceScore()
                        >= thresholds.concentrationDeploymentConfidenceThreshold()
                && input.asymmetryScore() >= thresholds.concentrationAsymmetryThreshold()
                && input.earlynessScore() >= thresholds.concentrationEarlynessThreshold();
    }

    private boolean standard(Verdict verdict, double score, CandidateValidationInput input) {
        return verdict == Verdict.PASS
                && score >= thresholds.standardDeploymentThreshold()
                && input.deploymentConfidenceScore() >= thresholds.standardDeploymentConfidenceThreshold();
    }

    private boolean probe(double score, CandidateValidationInput input) {
        return score >= thresholds.probeDeploymentThreshold()
                && input.deploymentConfidenceScore() >= thresholds.probeDeploymentConfidenceThreshold();
    }

    private boolean observe(double score) {
        return score >= thresholds.observeDeploymentThreshold();
    }
}
