package dev.reddragon.analytics.models;

import dev.reddragon.analytics.utilities.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized structural and material impact inputs.
 */
@Value
@Accessors(fluent = true)
public class FundamentalImpactSnapshot {
    double revenueImpactScore;
    double marketCapRelativeImpactScore;
    double structuralDemandShiftScore;
    double dilutionRiskScore;
    double insiderAlignmentScore;

    public FundamentalImpactSnapshot(
            double revenueImpactScore,
            double marketCapRelativeImpactScore,
            double structuralDemandShiftScore,
            double dilutionRiskScore,
            double insiderAlignmentScore
    ) {
        this.revenueImpactScore = AnalyticsScoreUtils.clamp(revenueImpactScore);
        this.marketCapRelativeImpactScore = AnalyticsScoreUtils.clamp(marketCapRelativeImpactScore);
        this.structuralDemandShiftScore = AnalyticsScoreUtils.clamp(structuralDemandShiftScore);
        this.dilutionRiskScore = AnalyticsScoreUtils.clamp(dilutionRiskScore);
        this.insiderAlignmentScore = AnalyticsScoreUtils.clamp(insiderAlignmentScore);
    }
}
