package dev.reddragon.marketdata.model;

import lombok.Builder;
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
    MarketDataQuality quality;
    List<String> notes;

    @Builder
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
        this.rangePosition = clamp(rangePosition);
        this.averageVolume = averageVolume;
        this.liquidityScore = clamp(liquidityScore);
        this.volatilityStabilityScore = clamp(volatilityStabilityScore);
        this.quality = quality == null ? MarketDataQuality.COMPLETE : quality;
        this.notes = List.copyOf(notes == null ? List.of() : notes);
    }

    public boolean complete() {
        return quality == MarketDataQuality.COMPLETE;
    }

    private static double clamp(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
