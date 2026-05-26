package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Realized volatility from close-to-close <b>log returns</b>, computed with
 * sample (Bessel-corrected) variance.
 *
 * <p><b>Per-bar vs annualized.</b> {@link #process(List)} returns the per-bar
 * standard deviation — useful when comparing across windows of the same
 * timeframe. {@link #processAnnualized(List, int)} scales by
 * {@code √barsPerYear} to produce the conventional annualized figure that
 * options pricing and regime classifiers expect (e.g. ~0.20 for a normal
 * equity, not ~0.001 per 5-minute bar). See lib-marketdata REVIEW.md
 * Finding #4 for the motivation.
 *
 * <p><b>Why log returns?</b> Log returns are time-additive — the sum of
 * log returns equals the log of the cumulative return. This is the
 * convention for variance calculations across multiple bars. For tiny
 * per-bar returns (|r| ≲ 1%) the numerical difference between log and
 * arithmetic returns is on the order of {@code r²/2}, i.e. negligible.
 *
 * <p><b>Bessel's correction.</b> Sample standard deviation divides by
 * {@code N − 1} rather than {@code N}, eliminating the downward bias when
 * the population mean is estimated from the sample (which is always the
 * case for a finite return series). The pre-correction implementation
 * (divide by N) had a small but real bias — calibration thresholds tuned
 * against the old value should be re-checked.
 */
public class RealizedVolatilityCalculator {

    /** Standard US-equity trading days per calendar year. */
    public static final int TRADING_DAYS_PER_YEAR = 252;

    /** 5-minute bars per trading session (6.5 h × 12 bars/h = 78). */
    public static final int FIVE_MINUTE_BARS_PER_SESSION = 78;

    /**
     * Per-bar realized volatility (sample standard deviation of log
     * returns). Returns {@code 0.0} when fewer than two bars are available
     * or when no valid (positive previous-close) returns can be computed.
     */
    public double process(List<IntradayBar> bars) {
        Objects.requireNonNull(bars, "bars are required");

        if (bars.size() < 2) {
            return 0.0;
        }

        List<Double> logReturns = logReturns(bars);
        return sampleStandardDeviation(logReturns);
    }

    /**
     * Annualized realized volatility. The per-bar standard deviation from
     * {@link #process(List)} is scaled by {@code √barsPerYear}.
     *
     * <p>Typical {@code barsPerYear} values:
     * <ul>
     *   <li>Daily bars: {@link #TRADING_DAYS_PER_YEAR} (252).</li>
     *   <li>Five-minute bars: {@code 252 × 78}.</li>
     *   <li>Hourly bars: {@code 252 × 7} (one bar per trading hour).</li>
     * </ul>
     *
     * @throws IllegalArgumentException if {@code barsPerYear < 1}.
     */
    public double processAnnualized(List<IntradayBar> bars, int barsPerYear) {
        if (barsPerYear < 1) {
            throw new IllegalArgumentException(
                    "barsPerYear must be >= 1, was: " + barsPerYear);
        }
        return process(bars) * Math.sqrt(barsPerYear);
    }

    private List<Double> logReturns(List<IntradayBar> bars) {
        List<Double> returns = new ArrayList<>(bars.size());

        for (int index = 1; index < bars.size(); index++) {
            double previousClose = bars.get(index - 1).close();
            double currentClose = bars.get(index).close();

            if (previousClose > 0.0 && currentClose > 0.0) {
                returns.add(Math.log(currentClose / previousClose));
            }
        }

        return returns;
    }

    /**
     * Sample standard deviation: divide by {@code N − 1} rather than
     * {@code N} (Bessel's correction). Returns {@code 0.0} for series with
     * fewer than two values, since sample std-dev is undefined there.
     */
    private double sampleStandardDeviation(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }

        double mean = 0.0;
        for (Double value : values) {
            mean += value;
        }
        mean /= values.size();

        double varianceTotal = 0.0;
        for (Double value : values) {
            double difference = value - mean;
            varianceTotal += difference * difference;
        }

        return Math.sqrt(varianceTotal / (values.size() - 1));
    }
}
