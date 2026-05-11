package dev.reddragon.analytics.model;

import dev.reddragon.analytics.util.AnalyticsScoreUtils;
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
        this.callPutImbalanceScore = AnalyticsScoreUtils.clamp(callPutImbalanceScore);
        this.unusualActivityScore = AnalyticsScoreUtils.clamp(unusualActivityScore);
        this.openInterestExpansionScore = AnalyticsScoreUtils.clamp(openInterestExpansionScore);
        this.nearMoneyFlowScore = AnalyticsScoreUtils.clamp(nearMoneyFlowScore);
        this.dealerPressureScore = AnalyticsScoreUtils.clamp(dealerPressureScore);
    }
}
