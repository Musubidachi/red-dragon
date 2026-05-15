package dev.reddragon.analytics.models;

import dev.reddragon.analytics.utilities.AnalyticsScoreUtils;
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
        this.mentionVelocityScore = AnalyticsScoreUtils.clamp(mentionVelocityScore);
        this.propagationAccelerationScore = AnalyticsScoreUtils.clamp(propagationAccelerationScore);
        this.crossPlatformExpansionScore = AnalyticsScoreUtils.clamp(crossPlatformExpansionScore);
        this.sectorSympathyScore = AnalyticsScoreUtils.clamp(sectorSympathyScore);
        this.narrativeCoherenceScore = AnalyticsScoreUtils.clamp(narrativeCoherenceScore);
    }
}
