package dev.reddragon.domain.models;

import dev.reddragon.math.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Normalized intraday structure inputs supplied to L4 scorers — the
 * <b>score-only</b> shape, no symbol/sessionVwap/latestClose context.
 *
 * <p>Produced from {@link MarketIntradayStructureSnapshot} (which carries the
 * provider context) before being passed into pure-function scorers in
 * {@code lib-analytics}. Two distinct types exist so the analytics layer
 * cannot accidentally depend on provider-side state.
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
