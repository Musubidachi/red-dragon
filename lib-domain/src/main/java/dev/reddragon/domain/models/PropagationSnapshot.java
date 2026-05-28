package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized propagation and reflexivity inputs.
 */
@Value
@Accessors(fluent = true)
public class PropagationSnapshot {
    double mentionVelocityScore;
    double propagationAccelerationScore;
    double crossPlatformExpansionScore;
    double sectorSympathyScore;
    double narrativeCoherenceScore;

    public PropagationSnapshot(
            double mentionVelocityScore,
            double propagationAccelerationScore,
            double crossPlatformExpansionScore,
            double sectorSympathyScore,
            double narrativeCoherenceScore
    ) {
        this.mentionVelocityScore = DomainScorePolicy.clampDerivedScore(mentionVelocityScore);
        this.propagationAccelerationScore = DomainScorePolicy.clampDerivedScore(propagationAccelerationScore);
        this.crossPlatformExpansionScore = DomainScorePolicy.clampDerivedScore(crossPlatformExpansionScore);
        this.sectorSympathyScore = DomainScorePolicy.clampDerivedScore(sectorSympathyScore);
        this.narrativeCoherenceScore = DomainScorePolicy.clampDerivedScore(narrativeCoherenceScore);
    }
}
