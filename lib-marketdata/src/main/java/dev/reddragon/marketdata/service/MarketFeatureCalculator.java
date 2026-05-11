package dev.reddragon.marketdata.service;

import dev.reddragon.marketdata.model.MarketBar;
import dev.reddragon.marketdata.model.MarketDataQuality;
import dev.reddragon.marketdata.model.MarketDataSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic market-feature calculator using daily bars.
 */
public class MarketFeatureCalculator {

    public MarketDataSnapshot calculate(String symbol, List<MarketBar> bars) {
        if (symbol == null || symbol.isBlank()) {
            return empty("", MarketDataQuality.MISSING_SYMBOL, "Symbol is missing.");
        }
        if (bars == null || bars.isEmpty()) {
            return empty(symbol, MarketDataQuality.EMPTY_BARS, "No market bars supplied.");
        }

        List<MarketBar> sorted = bars.stream()
                .sorted(Comparator.comparing(MarketBar::date))
                .toList();

        if (sorted.size() < 2) {
            return empty(symbol, MarketDataQuality.INSUFFICIENT_HISTORY, "At least two bars are required.");
        }

        MarketBar latest = sorted.get(sorted.size() - 1);
        MarketBar previous = sorted.get(sorted.size() - 2);
        List<String> notes = new ArrayList<>();

        double latestClose = latest.close();
        double previousClose = previous.close();
        double gapPercent = previousClose == 0 ? 0 : (latest.open() - previousClose) / previousClose;
        double atr = averageTrueRange(sorted);
        double rangePosition = latest.range() == 0 ? 0.5 : (latest.close() - latest.low()) / latest.range();
        double avgVolume = sorted.stream().mapToLong(MarketBar::volume).average().orElse(0.0);
        double liquidityScore = liquidityScore(avgVolume);
        double volatilityStabilityScore = volatilityStabilityScore(latestClose, atr);

        MarketDataQuality quality = MarketDataQuality.COMPLETE;
        if (sorted.size() < 14) {
            quality = MarketDataQuality.INSUFFICIENT_HISTORY;
            notes.add("Fewer than 14 bars supplied; ATR is usable but less stable.");
        }
        if (liquidityScore < 0.35) {
            quality = MarketDataQuality.ILLIQUID;
            notes.add("Average volume is low; liquidity risk is elevated.");
        }

        return MarketDataSnapshot.builder()
                .symbol(symbol)
                .observedAt(Instant.now())
                .latestClose(latestClose)
                .previousClose(previousClose)
                .gapPercent(gapPercent)
                .averageTrueRange(atr)
                .rangePosition(rangePosition)
                .averageVolume(avgVolume)
                .liquidityScore(liquidityScore)
                .volatilityStabilityScore(volatilityStabilityScore)
                .quality(quality)
                .notes(notes)
                .build();
    }

    private MarketDataSnapshot empty(String symbol, MarketDataQuality quality, String note) {
        return MarketDataSnapshot.builder()
                .symbol(symbol == null || symbol.isBlank() ? "UNKNOWN" : symbol)
                .observedAt(Instant.now())
                .quality(quality)
                .notes(List.of(note))
                .build();
    }

    private double averageTrueRange(List<MarketBar> sorted) {
        double total = 0.0;
        int count = 0;
        for (int i = 1; i < sorted.size(); i++) {
            MarketBar current = sorted.get(i);
            MarketBar previous = sorted.get(i - 1);
            double trueRange = Math.max(
                    current.high() - current.low(),
                    Math.max(
                            Math.abs(current.high() - previous.close()),
                            Math.abs(current.low() - previous.close())
                    )
            );
            total += trueRange;
            count++;
        }
        return count == 0 ? 0.0 : total / count;
    }

    private double liquidityScore(double averageVolume) {
        if (averageVolume >= 5_000_000) return 1.0;
        if (averageVolume >= 1_000_000) return 0.8;
        if (averageVolume >= 500_000) return 0.6;
        if (averageVolume >= 100_000) return 0.35;
        return 0.15;
    }

    private double volatilityStabilityScore(double latestClose, double atr) {
        if (latestClose <= 0 || atr <= 0) return 0.5;
        double atrPercent = atr / latestClose;
        if (atrPercent <= 0.03) return 0.9;
        if (atrPercent <= 0.06) return 0.75;
        if (atrPercent <= 0.10) return 0.55;
        if (atrPercent <= 0.15) return 0.35;
        return 0.15;
    }
}
