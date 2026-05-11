package dev.reddragon.analytics.model;

import dev.reddragon.analytics.util.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized intraday structure inputs supplied by market-data adapters.
 */
@Value
@Accessors(fluent = true)
public class IntradayStructureSnapshot {
    double vwapDistancePercent;
    double vwapReclaimStrength;
    double directionalPersistenceScore;
    double rotationalQualityScore;
    double intradayTrendStrength;
    boolean aboveVwap;

    public IntradayStructureSnapshot(
            double vwapDistancePercent,
            double vwapReclaimStrength,
            double directionalPersistenceScore,
            double rotationalQualityScore,
            double intradayTrendStrength,
            boolean aboveVwap
    ) {
        this.vwapDistancePercent = vwapDistancePercent;
        this.vwapReclaimStrength = AnalyticsScoreUtils.clamp(vwapReclaimStrength);
        this.directionalPersistenceScore = AnalyticsScoreUtils.clamp(directionalPersistenceScore);
        this.rotationalQualityScore = AnalyticsScoreUtils.clamp(rotationalQualityScore);
        this.intradayTrendStrength = AnalyticsScoreUtils.clamp(intradayTrendStrength);
        this.aboveVwap = aboveVwap;
    }
}
