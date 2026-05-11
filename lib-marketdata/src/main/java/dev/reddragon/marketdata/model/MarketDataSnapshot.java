package dev.reddragon.marketdata.model;

import java.time.Instant;
import java.util.List;

public record MarketDataSnapshot(
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
    public MarketDataSnapshot {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        symbol = symbol.trim().toUpperCase();
        observedAt = observedAt == null ? Instant.now() : observedAt;
        quality = quality == null ? MarketDataQuality.COMPLETE : quality;
        notes = List.copyOf(notes == null ? List.of() : notes);
        rangePosition = clamp(rangePosition);
        liquidityScore = clamp(liquidityScore);
        volatilityStabilityScore = clamp(volatilityStabilityScore);
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
