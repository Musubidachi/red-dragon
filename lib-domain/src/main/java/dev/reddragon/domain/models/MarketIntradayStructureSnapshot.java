package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Derived intraday structure metrics <b>with provider context</b>: symbol,
 * raw sessionVwap and latestClose, plus the normalized scores.
 *
 * <p>This is the <i>output</i> of {@code lib-marketdata}'s
 * {@code IntradayStructureSnapshotBuilder} and lives inside
 * {@link MarketDataSnapshot} or alongside it for persistence/review.
 *
 * <p>For the trimmed score-only shape consumed by L4 scorers in
 * {@code lib-analytics}, see {@link IntradayStructureSnapshot}.
 */
@Value
@Accessors(fluent = true)
public class MarketIntradayStructureSnapshot {
    String symbol;
    double sessionVwap;
    double latestClose;
    double vwapDistancePercent;
    double vwapReclaimStrength;
    double directionalPersistenceScore;
    double rotationalQualityScore;
    double intradayTrendStrength;
    boolean aboveVwap;

    public MarketIntradayStructureSnapshot(
            String symbol,
            double sessionVwap,
            double latestClose,
            double vwapDistancePercent,
            double vwapReclaimStrength,
            double directionalPersistenceScore,
            double rotationalQualityScore,
            double intradayTrendStrength,
            boolean aboveVwap
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        this.symbol = symbol.trim().toUpperCase();
        this.sessionVwap = sessionVwap;
        this.latestClose = latestClose;
        this.vwapDistancePercent = vwapDistancePercent;
        this.vwapReclaimStrength = DomainScorePolicy.clampDerivedScore(vwapReclaimStrength);
        this.directionalPersistenceScore = DomainScorePolicy.clampDerivedScore(directionalPersistenceScore);
        this.rotationalQualityScore = DomainScorePolicy.clampDerivedScore(rotationalQualityScore);
        this.intradayTrendStrength = DomainScorePolicy.clampDerivedScore(intradayTrendStrength);
        this.aboveVwap = aboveVwap;
    }
}
