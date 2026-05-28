package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Calculates volume-weighted average price from intraday bars.
 */
public class VwapCalculator {

    public static final ZoneId DEFAULT_SESSION_ZONE = ZoneId.of("America/New_York");

    /**
     * Cumulative VWAP across every supplied bar. Use
     * {@link #processSession(List, LocalDate, ZoneId)} when callers need an
     * exchange-session boundary.
     */
    public double process(List<IntradayBar> bars) {
        return processCumulative(bars);
    }

    /**
     * Cumulative VWAP across every supplied bar.
     */
    public double processCumulative(List<IntradayBar> bars) {
        Objects.requireNonNull(bars, "bars are required");
        return calculate(bars);
    }

    /**
     * VWAP for the explicit session date in the supplied session time zone.
     * Bars outside that session are ignored instead of being folded into a
     * multi-session cumulative VWAP.
     */
    public double processSession(List<IntradayBar> bars, LocalDate sessionDate, ZoneId sessionZone) {
        Objects.requireNonNull(bars, "bars are required");
        Objects.requireNonNull(sessionDate, "sessionDate is required");
        Objects.requireNonNull(sessionZone, "sessionZone is required");
        return calculate(sessionBars(bars, sessionDate, sessionZone));
    }

    /**
     * VWAP for bars that are already known to belong to one session.
     */
    public double processSingleSession(List<IntradayBar> bars, ZoneId sessionZone) {
        Objects.requireNonNull(bars, "bars are required");
        Objects.requireNonNull(sessionZone, "sessionZone is required");
        if (bars.isEmpty()) {
            return 0.0;
        }
        LocalDate sessionDate = sessionDate(bars.get(0), sessionZone);
        for (IntradayBar bar : bars) {
            if (!sessionDate.equals(sessionDate(bar, sessionZone))) {
                throw new IllegalArgumentException("Bars span multiple sessions");
            }
        }
        return calculate(bars);
    }

    private double calculate(List<IntradayBar> bars) {
        if (bars.isEmpty()) {
            return 0.0;
        }
        double weightedPriceTotal = weightedPriceTotal(bars);
        long volumeTotal = volumeTotal(bars);

        if (volumeTotal == 0) {
            return 0.0;
        }

        return weightedPriceTotal / volumeTotal;
    }

    private List<IntradayBar> sessionBars(List<IntradayBar> bars, LocalDate sessionDate, ZoneId sessionZone) {
        return bars.stream()
                .filter(bar -> sessionDate.equals(sessionDate(bar, sessionZone)))
                .toList();
    }

    private LocalDate sessionDate(IntradayBar bar, ZoneId sessionZone) {
        return bar.startTime().atZone(sessionZone).toLocalDate();
    }

    private double weightedPriceTotal(List<IntradayBar> bars) {
        double total = 0.0;

        for (IntradayBar bar : bars) {
            total += typicalPrice(bar) * bar.volume();
        }

        return total;
    }

    private long volumeTotal(List<IntradayBar> bars) {
        long total = 0L;

        for (IntradayBar bar : bars) {
            total += bar.volume();
        }

        return total;
    }

    private double typicalPrice(IntradayBar bar) {
        return (bar.high() + bar.low() + bar.close()) / 3.0;
    }
}
