package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
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
        this.revenueImpactScore = DomainScorePolicy.clampDerivedScore(revenueImpactScore);
        this.marketCapRelativeImpactScore = DomainScorePolicy.clampDerivedScore(marketCapRelativeImpactScore);
        this.structuralDemandShiftScore = DomainScorePolicy.clampDerivedScore(structuralDemandShiftScore);
        this.dilutionRiskScore = DomainScorePolicy.clampDerivedScore(dilutionRiskScore);
        this.insiderAlignmentScore = DomainScorePolicy.clampDerivedScore(insiderAlignmentScore);
    }
}
