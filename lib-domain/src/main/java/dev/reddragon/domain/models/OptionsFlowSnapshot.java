package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized options-flow inputs.
 */
@Value
@Accessors(fluent = true)
public class OptionsFlowSnapshot {
    double callPutImbalanceScore;
    double unusualActivityScore;
    double openInterestExpansionScore;
    double nearMoneyFlowScore;
    double dealerPressureScore;

    public OptionsFlowSnapshot(
            double callPutImbalanceScore,
            double unusualActivityScore,
            double openInterestExpansionScore,
            double nearMoneyFlowScore,
            double dealerPressureScore
    ) {
        this.callPutImbalanceScore = DomainScorePolicy.clampDerivedScore(callPutImbalanceScore);
        this.unusualActivityScore = DomainScorePolicy.clampDerivedScore(unusualActivityScore);
        this.openInterestExpansionScore = DomainScorePolicy.clampDerivedScore(openInterestExpansionScore);
        this.nearMoneyFlowScore = DomainScorePolicy.clampDerivedScore(nearMoneyFlowScore);
        this.dealerPressureScore = DomainScorePolicy.clampDerivedScore(dealerPressureScore);
    }
}
