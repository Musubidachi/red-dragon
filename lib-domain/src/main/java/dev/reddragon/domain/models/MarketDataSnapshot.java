package dev.reddragon.domain.models;

import dev.reddragon.domain.utilities.DomainScorePolicy;
import dev.reddragon.math.MarketMathUtils;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Value
@Accessors(fluent = true)
public class MarketDataSnapshot {
    public static final double HOSTILE_VOLATILITY_STABILITY_MAX = 0.25;

    String symbol;
    Instant observedAt;
    double latestClose;
    double previousClose;
    double gapPercent;
    double averageTrueRange;
    double rangePosition;
    double averageVolume;
    double liquidityScore;
    double volatilityStabilityScore;
    /** Ratio of the latest bar's volume to the average volume (1.0 = average). */
    double relativeVolume;
    /** Deviation of the latest close from the VWAP, expressed as a fraction of close. */
    double vwapDeviation;
    /**
     * Fraction of bars (0.0–1.0) that moved in the same direction as the net
     * price trend over the lookback window.  Higher values indicate persistent
     * directional momentum.
     */
    double directionalPersistence;
    MarketDataQuality quality;
    List<String> notes;

    public MarketDataSnapshot(
            String symbol,
            Instant observedAt,
            double latestClose,
            double previousClose,
            double gapPercent,
            double averageTrueRange,
            double rangePosition,
            double averageVolume,
            double liquidityScore,
            double volatilityStabilityScore,
            double relativeVolume,
            double vwapDeviation,
            double directionalPersistence,
            MarketDataQuality quality,
            List<String> notes
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        this.symbol = symbol.trim().toUpperCase();
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt is required");
        this.latestClose = requireFiniteNonNegative("latestClose", latestClose);
        this.previousClose = requireFiniteNonNegative("previousClose", previousClose);
        this.gapPercent = requireFinite("gapPercent", gapPercent);
        this.averageTrueRange = requireFiniteNonNegative("averageTrueRange", averageTrueRange);
        this.rangePosition = DomainScorePolicy.clampDerivedScore(rangePosition);
        this.averageVolume = requireFiniteNonNegative("averageVolume", averageVolume);
        this.liquidityScore = DomainScorePolicy.clampDerivedScore(liquidityScore);
        this.volatilityStabilityScore = DomainScorePolicy.clampDerivedScore(volatilityStabilityScore);
        this.relativeVolume = MarketMathUtils.floorAtZero(requireFinite("relativeVolume", relativeVolume));
        this.vwapDeviation = requireFinite("vwapDeviation", vwapDeviation);
        this.directionalPersistence = DomainScorePolicy.clampDerivedScore(directionalPersistence);
        this.quality = quality == null ? MarketDataQuality.COMPLETE : quality;
        this.notes = List.copyOf(notes == null ? List.of() : notes);
    }

    /**
     * Reject {@code NaN}, {@code ±Infinity}, and negative values. Prices,
     * volumes, and ATR are physically non-negative; a negative value here
     * is a programmer error, not a market condition.
     */
    private static double requireFiniteNonNegative(String fieldName, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        if (value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    /**
     * Reject {@code NaN} and {@code ±Infinity}. Gap percent and vwap
     * deviation can legitimately be negative; only non-finite values are
     * a programmer error.
     */
    private static double requireFinite(String fieldName, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        return value;
    }

    public boolean complete() {
        return quality == MarketDataQuality.COMPLETE;
    }

    /**
     * Return a new snapshot identical to this one except that the two
     * score fields are replaced with the supplied values. Used by the
     * lib-analytics {@code MarketDataSnapshotScorer} to enrich a raw
     * marketdata-emitted snapshot (which carries score=0 placeholders)
     * with the scoring step's computed values. See lib-marketdata
     * REVIEW.md Finding #8 — scoring lives in L4 ({@code lib-analytics}),
     * marketdata only emits raw features.
     */
    public MarketDataSnapshot withScores(double liquidityScore, double volatilityStabilityScore) {
        return new MarketDataSnapshot(
                symbol, observedAt, latestClose, previousClose, gapPercent,
                averageTrueRange, rangePosition, averageVolume,
                liquidityScore, volatilityStabilityScore,
                relativeVolume, vwapDeviation, directionalPersistence,
                quality, notes);
    }

    /**
     * Returns {@code true} if the current market conditions are hostile — either
     * illiquid, stale, or the volatility stability score is in the bottom quartile.
     */
    public boolean isHostile() {
        return quality == MarketDataQuality.ILLIQUID
                || quality == MarketDataQuality.STALE
                || volatilityStabilityScore < HOSTILE_VOLATILITY_STABILITY_MAX;
    }

    /**
     * Classifies the symbol into a liquidity tier based on average daily volume.
     * Thresholds are intentionally conservative — small-cap candidates require
     * careful sizing even in the ADEQUATE tier.
     *
     * @return the matching closed-set liquidity tier
     */
    public LiquidityTier liquidityTier() {
        return LiquidityTier.fromAverageVolume(averageVolume);
    }
}
