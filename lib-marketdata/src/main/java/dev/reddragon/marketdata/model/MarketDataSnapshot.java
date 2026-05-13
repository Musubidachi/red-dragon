package dev.reddragon.marketdata.model;

import dev.reddragon.marketdata.util.MarketMathUtils;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

@Value
@Accessors(fluent = true)
public class MarketDataSnapshot {
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
        this.observedAt = observedAt == null ? Instant.now() : observedAt;
        this.latestClose = latestClose;
        this.previousClose = previousClose;
        this.gapPercent = gapPercent;
        this.averageTrueRange = averageTrueRange;
        this.rangePosition = MarketMathUtils.clamp(rangePosition);
        this.averageVolume = averageVolume;
        this.liquidityScore = MarketMathUtils.clamp(liquidityScore);
        this.volatilityStabilityScore = MarketMathUtils.clamp(volatilityStabilityScore);
        this.relativeVolume = Math.max(0.0, relativeVolume);
        this.vwapDeviation = vwapDeviation;
        this.directionalPersistence = MarketMathUtils.clamp(directionalPersistence);
        this.quality = quality == null ? MarketDataQuality.COMPLETE : quality;
        this.notes = List.copyOf(notes == null ? List.of() : notes);
    }

    public boolean complete() {
        return quality == MarketDataQuality.COMPLETE;
    }

    /**
     * Returns {@code true} if the current market conditions are hostile — either
     * illiquid, stale, or the volatility stability score is in the bottom quartile.
     */
    public boolean isHostile() {
        return quality == MarketDataQuality.ILLIQUID
                || quality == MarketDataQuality.STALE
                || volatilityStabilityScore < 0.25;
    }

    /**
     * Classifies the symbol into a liquidity tier based on average daily volume.
     * Thresholds are intentionally conservative — small-cap candidates require
     * careful sizing even in the ADEQUATE tier.
     *
     * @return "HIGH" (&gt;1M), "MODERATE" (&gt;250k), "ADEQUATE" (&gt;50k), or "THIN"
     */
    public String liquidityTier() {
        if (averageVolume >= 1_000_000) return "HIGH";
        if (averageVolume >= 250_000)   return "MODERATE";
        if (averageVolume >= 50_000)    return "ADEQUATE";
        return "THIN";
    }
}
