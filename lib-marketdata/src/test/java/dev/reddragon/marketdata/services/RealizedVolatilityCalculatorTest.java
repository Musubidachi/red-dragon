package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Bessel-corrected, log-return-based realized volatility
 * calculator. The per-bar value should be small ({@code ≪ 1}); the
 * annualized overload scales it up by {@code √barsPerYear}.
 */
class RealizedVolatilityCalculatorTest {

    private final RealizedVolatilityCalculator calculator = new RealizedVolatilityCalculator();

    @Test
    void nullBarsRejected() {
        assertThrows(NullPointerException.class, () -> calculator.process(null));
    }

    @Test
    void emptyBarsReturnZero() {
        assertEquals(0.0, calculator.process(List.of()));
    }

    @Test
    void singleBarReturnsZero() {
        assertEquals(0.0, calculator.process(List.of(bar(0, 100))));
    }

    @Test
    void constantPriceReturnsZeroVolatility() {
        // Identical closes => zero returns => zero std dev.
        List<IntradayBar> bars = List.of(
                bar(0,   100.0),
                bar(60,  100.0),
                bar(120, 100.0),
                bar(180, 100.0)
        );
        assertEquals(0.0, calculator.process(bars), 1e-12);
    }

    @Test
    void positiveVolatilityMatchesSampleStdDevOfLogReturns() {
        // Closes: 100 -> 102 -> 100 -> 103
        // Log returns:
        //   r1 = ln(102/100) ≈  0.01980263
        //   r2 = ln(100/102) ≈ -0.01980263
        //   r3 = ln(103/100) ≈  0.02955880
        //   mean ≈ 0.00985293
        //   sample variance (Bessel, divide by N-1=2) ≈ 6.834e-4
        //   sigma = sqrt(sample variance) ≈ 0.0261426
        List<IntradayBar> bars = List.of(
                bar(0,   100.0),
                bar(60,  102.0),
                bar(120, 100.0),
                bar(180, 103.0)
        );
        double sigma = calculator.process(bars);
        assertEquals(0.0261426, sigma, 5e-5);
        assertTrue(sigma > 0.0);
    }

    @Test
    void annualizedScalesBySqrtBarsPerYear() {
        // For any fixture, processAnnualized(bars, n) must equal process(bars) * sqrt(n).
        List<IntradayBar> bars = List.of(
                bar(0,   100.0),
                bar(60,  102.0),
                bar(120, 100.0),
                bar(180, 103.0)
        );
        double perBar = calculator.process(bars);
        double annualized = calculator.processAnnualized(bars,
                RealizedVolatilityCalculator.TRADING_DAYS_PER_YEAR);
        assertEquals(perBar * Math.sqrt(RealizedVolatilityCalculator.TRADING_DAYS_PER_YEAR),
                annualized, 1e-12);
    }

    @Test
    void annualizedRejectsNonPositiveBarsPerYear() {
        List<IntradayBar> bars = List.of(bar(0, 100.0), bar(60, 101.0));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.processAnnualized(bars, 0));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.processAnnualized(bars, -1));
    }

    @Test
    void zeroOrNegativeClosesAreSkippedDefensively() {
        // A bar with non-positive close cannot contribute a log return — the
        // calculator should silently skip it instead of producing NaN.
        List<IntradayBar> bars = List.of(
                bar(0,   100.0),
                bar(60,    0.0),     // skipped: previous close = 100, current = 0
                bar(120, 100.0),     // skipped: previous close = 0
                bar(180, 102.0)
        );
        double sigma = calculator.process(bars);
        assertTrue(Double.isFinite(sigma), "sigma must be finite, got " + sigma);
        assertTrue(sigma >= 0.0);
    }

    @Test
    void singleValidReturnYieldsZeroSampleStdDev() {
        // Two bars => one log return => sample std dev is undefined; we
        // return 0 instead of NaN.
        List<IntradayBar> bars = List.of(bar(0, 100.0), bar(60, 102.0));
        assertEquals(0.0, calculator.process(bars), 1e-12);
    }

    private IntradayBar bar(long offsetSec, double close) {
        // We only care about close for vol; OHLC + vwap padded with sane values.
        double high = Math.max(close * 1.001, close + 0.01);
        double low = Math.max(0.0, Math.min(close * 0.999, close - 0.01));
        return new IntradayBar(
                "X",
                Instant.parse("2026-05-13T13:30:00Z").plusSeconds(offsetSec),
                close, high, low, close, 1_000L, close
        );
    }
}
