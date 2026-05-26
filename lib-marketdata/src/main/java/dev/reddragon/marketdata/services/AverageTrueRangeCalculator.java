package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.OhlcBar;

import java.util.List;
import java.util.Objects;

/**
 * Calculates the Average True Range (ATR) of a series of bars using Wilder's
 * smoothed moving average (RMA), the convention used in technical analysis
 * tooling since J. Welles Wilder's original 1978 publication.
 *
 * <p><b>Algorithm.</b> The true range (TR) at bar <i>i</i> is the max of:
 * <ul>
 *   <li>{@code high<sub>i</sub> - low<sub>i</sub>}</li>
 *   <li>{@code |high<sub>i</sub> - close<sub>i-1</sub>|}</li>
 *   <li>{@code |low<sub>i</sub> - close<sub>i-1</sub>|}</li>
 * </ul>
 * Wilder smoothing then evolves the ATR forward:
 * <pre>
 *   ATR<sub>period</sub> = mean(TR<sub>1</sub> … TR<sub>period</sub>)            // seed
 *   ATR<sub>n</sub>      = (ATR<sub>n-1</sub> × (period − 1) + TR<sub>n</sub>) / period  // n > period
 * </pre>
 *
 * <p><b>Backwards-compatible fallback.</b> When the input contains fewer
 * than {@code period + 1} bars (i.e. fewer than {@code period} true-range
 * samples to seed the RMA), the calculator falls back to the arithmetic
 * mean of whatever TRs it has. This preserves the pre-Wilder behaviour
 * for short series and keeps existing call sites with small fixtures
 * working — see lib-marketdata REVIEW.md Finding #3.
 *
 * <p><b>OhlcBar contract.</b> The calculator accepts any
 * {@link OhlcBar} — daily {@code MarketBar} and intraday {@code IntradayBar}
 * both qualify, so callers no longer need a per-bar-type wrapper or
 * inline reimplementation. See lib-marketdata REVIEW.md Finding #2.
 */
public class AverageTrueRangeCalculator {

    /** Wilder's standard ATR lookback. */
    public static final int DEFAULT_PERIOD = 14;

    /**
     * Main processing flow with the default 14-bar Wilder smoothing.
     */
    public double process(List<? extends OhlcBar> bars) {
        return process(bars, DEFAULT_PERIOD);
    }

    /**
     * Wilder-smoothed ATR over {@code period} bars. When the series is too
     * short to seed the RMA, falls back to the arithmetic mean of all
     * available true ranges.
     *
     * @throws IllegalArgumentException if {@code period < 1}.
     */
    public double process(List<? extends OhlcBar> bars, int period) {
        Objects.requireNonNull(bars, "bars are required");
        if (period < 1) {
            throw new IllegalArgumentException("period must be >= 1, was: " + period);
        }

        if (bars.size() < 2) {
            return 0.0;
        }

        int trCount = bars.size() - 1;
        if (trCount < period) {
            return arithmeticMeanTrueRange(bars);
        }

        // Wilder smoothing: seed from the mean of the first `period` TRs,
        // then evolve.
        double seed = 0.0;
        for (int index = 1; index <= period; index++) {
            seed += trueRange(bars.get(index), bars.get(index - 1));
        }
        double atr = seed / period;

        for (int index = period + 1; index < bars.size(); index++) {
            double tr = trueRange(bars.get(index), bars.get(index - 1));
            atr = ((atr * (period - 1)) + tr) / period;
        }
        return atr;
    }

    /**
     * Arithmetic mean of every true range in the series. Public so callers
     * that explicitly want the pre-Wilder behaviour (e.g. comparing legacy
     * snapshots) can opt in.
     */
    public double arithmeticMeanTrueRange(List<? extends OhlcBar> bars) {
        Objects.requireNonNull(bars, "bars are required");
        if (bars.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int index = 1; index < bars.size(); index++) {
            total += trueRange(bars.get(index), bars.get(index - 1));
        }
        return total / (bars.size() - 1);
    }

    private double trueRange(OhlcBar current, OhlcBar previous) {
        double highLow = current.high() - current.low();
        double highClose = Math.abs(current.high() - previous.close());
        double lowClose = Math.abs(current.low() - previous.close());

        return Math.max(highLow, Math.max(highClose, lowClose));
    }
}
