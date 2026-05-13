package dev.reddragon.marketdata.service;

import dev.reddragon.marketdata.model.MarketBar;
import dev.reddragon.marketdata.model.MarketDataQuality;
import dev.reddragon.marketdata.model.MarketDataSnapshot;
import dev.reddragon.marketdata.util.MarketMathUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates deterministic market features from daily bars.
 */
public class MarketFeatureCalculator {

    /**
     * Main processing flow.
     */
    public MarketDataSnapshot process(String symbol, List<MarketBar> bars) {
        if (missingSymbol(symbol)) {
            return emptySnapshot("UNKNOWN", MarketDataQuality.MISSING_SYMBOL, "Symbol is missing.");
        }

        if (missingBars(bars)) {
            return emptySnapshot(symbol, MarketDataQuality.EMPTY_BARS, "No market bars supplied.");
        }

        List<MarketBar> sortedBars = sortBars(bars);
        if (insufficientHistory(sortedBars)) {
            return emptySnapshot(symbol, MarketDataQuality.INSUFFICIENT_HISTORY, "At least two bars are required.");
        }

        return buildSnapshot(symbol, sortedBars);
    }

    private boolean missingSymbol(String symbol) {
        return symbol == null || symbol.isBlank();
    }

    private boolean missingBars(List<MarketBar> bars) {
        return bars == null || bars.isEmpty();
    }

    private List<MarketBar> sortBars(List<MarketBar> bars) {
        return bars.stream()
                .sorted(Comparator.comparing(MarketBar::date))
                .toList();
    }

    private boolean insufficientHistory(List<MarketBar> bars) {
        return bars.size() < 2;
    }

    private MarketDataSnapshot buildSnapshot(String symbol, List<MarketBar> bars) {
        MarketBar latestBar = latestBar(bars);
        MarketBar previousBar = previousBar(bars);

        double latestClose = latestBar.close();
        double previousClose = previousBar.close();
        double gapPercent = gapPercent(latestBar, previousClose);
        double averageTrueRange = averageTrueRange(bars);
        double rangePosition = rangePosition(latestBar);
        double averageVolume = averageVolume(bars);
        double liquidityScore = liquidityScore(averageVolume);
        double volatilityStabilityScore = volatilityStabilityScore(latestClose, averageTrueRange);
        double relativeVolume = relativeVolume(latestBar, averageVolume);
        double vwapDeviation = vwapDeviation(bars, latestClose);
        double directionalPersistence = directionalPersistence(bars);

        List<String> notes = notes(bars, liquidityScore);
        MarketDataQuality quality = quality(bars, liquidityScore);

        return new MarketDataSnapshot(
                symbol,
                Instant.now(),
                latestClose,
                previousClose,
                gapPercent,
                averageTrueRange,
                rangePosition,
                averageVolume,
                liquidityScore,
                volatilityStabilityScore,
                relativeVolume,
                vwapDeviation,
                directionalPersistence,
                quality,
                notes
        );
    }

    private MarketBar latestBar(List<MarketBar> bars) {
        return bars.get(bars.size() - 1);
    }

    private MarketBar previousBar(List<MarketBar> bars) {
        return bars.get(bars.size() - 2);
    }

    private double gapPercent(MarketBar latestBar, double previousClose) {
        return MarketMathUtils.safePercentChange(latestBar.open(), previousClose);
    }

    private double rangePosition(MarketBar latestBar) {
        if (latestBar.range() == 0) {
            return 0.5;
        }

        return (latestBar.close() - latestBar.low()) / latestBar.range();
    }

    private double averageVolume(List<MarketBar> bars) {
        return bars.stream()
                .mapToLong(MarketBar::volume)
                .average()
                .orElse(0.0);
    }

    private List<String> notes(List<MarketBar> bars, double liquidityScore) {
        List<String> notes = new ArrayList<>();

        if (bars.size() < 14) {
            notes.add("Fewer than 14 bars supplied; ATR is usable but less stable.");
        }

        if (liquidityScore < 0.35) {
            notes.add("Average volume is low; liquidity risk is elevated.");
        }

        return notes;
    }

    private MarketDataQuality quality(List<MarketBar> bars, double liquidityScore) {
        if (liquidityScore < 0.35) {
            return MarketDataQuality.ILLIQUID;
        }

        if (bars.size() < 14) {
            return MarketDataQuality.INSUFFICIENT_HISTORY;
        }

        return MarketDataQuality.COMPLETE;
    }

    private MarketDataSnapshot emptySnapshot(
            String symbol,
            MarketDataQuality quality,
            String note
    ) {
        return new MarketDataSnapshot(
                symbol,
                Instant.now(),
                0, 0, 0, 0, 0, 0, 0, 0,
                0.0, 0.0, 0.0,
                quality,
                List.of(note)
        );
    }

    /**
     * Ratio of the latest bar's volume to the period average.
     * Returns 1.0 when average is zero to avoid division by zero.
     */
    private double relativeVolume(MarketBar latestBar, double averageVolume) {
        if (averageVolume == 0) return 1.0;
        return latestBar.volume() / averageVolume;
    }

    /**
     * (latestClose - VWAP) / latestClose where VWAP = Σ(close × volume) / Σvolume.
     * Returns 0.0 when total volume is zero.
     */
    private double vwapDeviation(List<MarketBar> bars, double latestClose) {
        double totalVolume = 0.0;
        double totalValue = 0.0;
        for (MarketBar bar : bars) {
            totalVolume += bar.volume();
            totalValue  += bar.close() * bar.volume();
        }
        if (totalVolume == 0 || latestClose == 0) return 0.0;
        double vwap = totalValue / totalVolume;
        return (latestClose - vwap) / latestClose;
    }

    /**
     * Fraction of consecutive bar pairs where both bars moved in the same direction
     * (both up or both down).  Measures momentum persistence across the lookback window.
     */
    private double directionalPersistence(List<MarketBar> bars) {
        if (bars.size() < 2) return 0.5;
        int consistent = 0;
        int total = 0;
        for (int i = 1; i < bars.size(); i++) {
            double prev = bars.get(i - 1).close();
            double curr = bars.get(i).close();
            if (i >= 2) {
                double prevPrev = bars.get(i - 2).close();
                boolean prevUp = curr > prev;
                boolean currUp = prev > prevPrev;
                if (prevUp == currUp) consistent++;
                total++;
            }
        }
        return total == 0 ? 0.5 : (double) consistent / total;
    }

    private double averageTrueRange(List<MarketBar> bars) {
        double total = 0.0;
        int count = 0;

        for (int index = 1; index < bars.size(); index++) {
            total += trueRange(bars.get(index), bars.get(index - 1));
            count++;
        }

        return count == 0 ? 0.0 : total / count;
    }

    private double trueRange(MarketBar currentBar, MarketBar previousBar) {
        return Math.max(
                currentBar.high() - currentBar.low(),
                Math.max(
                        Math.abs(currentBar.high() - previousBar.close()),
                        Math.abs(currentBar.low() - previousBar.close())
                )
        );
    }

    private double liquidityScore(double averageVolume) {
        if (averageVolume >= 5_000_000) {
            return 1.0;
        }
        if (averageVolume >= 1_000_000) {
            return 0.8;
        }
        if (averageVolume >= 500_000) {
            return 0.6;
        }
        if (averageVolume >= 100_000) {
            return 0.35;
        }
        return 0.15;
    }

    private double volatilityStabilityScore(double latestClose, double averageTrueRange) {
        if (latestClose <= 0 || averageTrueRange <= 0) {
            return 0.5;
        }

        double atrPercent = averageTrueRange / latestClose;

        if (atrPercent <= 0.03) {
            return 0.9;
        }
        if (atrPercent <= 0.06) {
            return 0.75;
        }
        if (atrPercent <= 0.10) {
            return 0.55;
        }
        if (atrPercent <= 0.15) {
            return 0.35;
        }

        return 0.15;
    }
}
