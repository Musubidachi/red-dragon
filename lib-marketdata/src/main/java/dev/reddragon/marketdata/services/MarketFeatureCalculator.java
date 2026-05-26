package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.math.MarketMathUtils;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates deterministic market features from daily bars.
 *
 * <p>ATR is delegated to {@link AverageTrueRangeCalculator} via the shared
 * {@code OhlcBar} contract — the previous inline reimplementation has been
 * removed (lib-marketdata REVIEW.md Finding #2). The delegate now uses
 * Wilder smoothing rather than a flat arithmetic mean (Finding #3), so
 * downstream {@code volatilityStabilityScore} thresholds calibrated
 * against the old simple-mean values may need a re-validation pass.
 *
 * <p><b>Scoring is no longer this class's responsibility.</b> The
 * snapshot's {@code liquidityScore} and {@code volatilityStabilityScore}
 * fields are emitted as {@code 0.0} placeholders; the lib-analytics
 * {@code MarketDataSnapshotScorer} fills them in immediately downstream.
 * See lib-marketdata REVIEW.md Finding #8 for the architectural rationale.
 * The ILLIQUID quality flag is now driven by the raw
 * {@code averageVolume} feature directly, eliminating the need for
 * scoring policy inside this class.
 */
public class MarketFeatureCalculator {

    /**
     * Shared ATR calculator. Created lazily here instead of injected so
     * this class can stay constructor-free for the moment (the broader
     * Lombok-service rollout is REVIEW.md Finding #7, tracked separately).
     */
    private final AverageTrueRangeCalculator atrCalculator = new AverageTrueRangeCalculator();

    /**
     * Raw-feature volume floor below which a symbol is flagged
     * {@link MarketDataQuality#ILLIQUID}. Tracks the historical
     * {@code liquidityVolumeLow = 100_000} threshold that used to drive
     * the score-based check. Intentionally a feature-level threshold,
     * not a score-level one: data-quality decisions belong here in L2;
     * scoring belongs in L4 (lib-analytics).
     */
    private static final long ILLIQUID_VOLUME_FLOOR = 100_000;

    /**
     * Placeholder value the snapshot carries for the two score fields
     * until the lib-analytics enrichment step rewrites them. Downstream
     * code that runs before enrichment should not read these — they are
     * not meaningful until {@code MarketDataSnapshotScorer.process} has
     * been applied.
     */
    private static final double SCORE_PLACEHOLDER = 0.0;

    // ---- Sample-size / data-quality minimums -------------------------------
    private static final int    MIN_BARS_FOR_STABLE_ATR               = 14;

    public MarketFeatureCalculator() {
        // No scoring dependencies — scoring is the analytics layer's job
        // (REVIEW.md Finding #8 architectural move).
    }

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
        double relativeVolume = relativeVolume(latestBar, averageVolume);
        double vwapDeviation = vwapDeviation(bars, latestClose);
        double directionalPersistence = directionalPersistence(bars);

        List<String> notes = notes(bars, averageVolume);
        MarketDataQuality quality = quality(bars, averageVolume);

        // liquidityScore and volatilityStabilityScore are placeholders here —
        // the lib-analytics MarketDataSnapshotScorer enrichment step writes
        // the real values immediately downstream. See REVIEW.md Finding #8.
        return new MarketDataSnapshot(
                symbol,
                observedAt(latestBar),
                latestClose,
                previousClose,
                gapPercent,
                averageTrueRange,
                rangePosition,
                averageVolume,
                SCORE_PLACEHOLDER,
                SCORE_PLACEHOLDER,
                relativeVolume,
                vwapDeviation,
                directionalPersistence,
                quality,
                notes
        );
    }

    /**
     * Anchor the snapshot's observation timestamp to the latest bar's date so
     * a replay of the same bars produces a snapshot equal to the original.
     * Daily bars carry only a {@link java.time.LocalDate}; we stamp the
     * conventional 21:00 UTC ≈ 16:00 New York close as the observation time.
     */
    private Instant observedAt(MarketBar latestBar) {
        return latestBar.date().atTime(LocalTime.of(21, 0)).toInstant(ZoneOffset.UTC);
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

    /**
     * Where the close sits within the bar's range, on {@code [0.0, 1.0]}.
     *
     * <p>Returns {@code 0.5} as the "no information" sentinel when the bar
     * has zero range (high == low). The sentinel is the midpoint by design:
     * we don't know where the close was, so we don't push the score in
     * either direction.
     */
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

    private List<String> notes(List<MarketBar> bars, double averageVolume) {
        List<String> notes = new ArrayList<>();

        if (bars.size() < MIN_BARS_FOR_STABLE_ATR) {
            notes.add("Fewer than " + MIN_BARS_FOR_STABLE_ATR
                    + " bars supplied; ATR is usable but less stable.");
        }

        if (averageVolume < ILLIQUID_VOLUME_FLOOR) {
            notes.add("Average volume is low; liquidity risk is elevated.");
        }

        return notes;
    }

    private MarketDataQuality quality(List<MarketBar> bars, double averageVolume) {
        if (averageVolume < ILLIQUID_VOLUME_FLOOR) {
            return MarketDataQuality.ILLIQUID;
        }

        if (bars.size() < MIN_BARS_FOR_STABLE_ATR) {
            return MarketDataQuality.INSUFFICIENT_HISTORY;
        }

        return MarketDataQuality.COMPLETE;
    }

    /**
     * Build an empty-but-typed snapshot for an error path (missing symbol,
     * empty bars, insufficient history). Stamps {@code Instant.now()} because
     * we have no bar to anchor to; deterministic replay through this path is
     * not meaningful — the quality enum is what callers branch on.
     */
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
     *
     * <p>Returns {@code 1.0} (= "exactly average") as the "no information"
     * sentinel when the period average is zero — i.e. we report the latest
     * day as neither heavy nor light without data to claim otherwise. Note
     * this differs from {@link #rangePosition} (0.5) and {@link #vwapDeviation}
     * (0.0) — each helper uses a "neutral within its own scale" sentinel.
     */
    private double relativeVolume(MarketBar latestBar, double averageVolume) {
        if (averageVolume == 0) return 1.0;
        return latestBar.volume() / averageVolume;
    }

    /**
     * {@code (latestClose - VWAP) / latestClose} where {@code VWAP = Σ(close × volume) / Σvolume}.
     *
     * <p>Returns {@code 0.0} as the "no deviation" sentinel when total volume
     * or latest close is zero. {@code 0.0} is neutral because the field is
     * already a signed deviation around zero.
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
     * Fraction of consecutive bar pairs where both bars moved in the same
     * direction (both up or both down). Measures momentum persistence across
     * the lookback window.
     *
     * <p>Returns {@code 0.5} as the "no information" sentinel when fewer than
     * 3 bars are supplied (we need at least 3 closes to form two consecutive
     * direction comparisons).
     */
    private double directionalPersistence(List<MarketBar> bars) {
        if (bars.size() < 3) return 0.5;
        int consistent = 0;
        int total = 0;
        for (int i = 2; i < bars.size(); i++) {
            double olderClose  = bars.get(i - 2).close();
            double middleClose = bars.get(i - 1).close();
            double recentClose = bars.get(i).close();
            boolean recentUp = recentClose > middleClose;
            boolean olderUp  = middleClose > olderClose;
            if (recentUp == olderUp) consistent++;
            total++;
        }
        return total == 0 ? 0.5 : (double) consistent / total;
    }

    /**
     * Delegates to the shared {@link AverageTrueRangeCalculator}. Daily
     * series passed by {@link #process(String, List)} typically contain
     * many more than {@link AverageTrueRangeCalculator#DEFAULT_PERIOD}
     * bars, so Wilder smoothing engages; for shorter fixtures the
     * delegate falls back to the arithmetic mean automatically.
     */
    private double averageTrueRange(List<MarketBar> bars) {
        return atrCalculator.process(bars);
    }

    // Scoring methods removed — see lib-marketdata REVIEW.md Finding #8.
    // The lib-analytics MarketDataSnapshotScorer owns the policy.
}
