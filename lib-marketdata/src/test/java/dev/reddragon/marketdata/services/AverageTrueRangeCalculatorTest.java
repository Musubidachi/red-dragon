package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AverageTrueRangeCalculatorTest {

    private final AverageTrueRangeCalculator calculator = new AverageTrueRangeCalculator();

    @Test
    void nullBarsRejected() {
        assertThrows(NullPointerException.class, () -> calculator.process(null));
    }

    @Test
    void singleBarReturnsZero() {
        IntradayBar bar = bar(0, 100, 102, 98, 100);
        assertEquals(0.0, calculator.process(List.of(bar)));
    }

    @Test
    void shortSeriesFallsBackToArithmeticMean() {
        // 3 bars → 2 TRs, which is < default period (14). Calculator falls
        // back to the simple arithmetic mean of true ranges to preserve the
        // pre-Wilder behaviour for short fixtures.
        // bar1: H=102 L=98 close=100
        // bar2: H=103 L=97 close=99  -> TR = max(6, |103-100|=3, |97-100|=3)  = 6
        // bar3: H=105 L=101 close=104 -> TR = max(4, |105-99|=6, |101-99|=2)  = 6
        // ATR = (6 + 6) / 2 = 6
        List<IntradayBar> bars = List.of(
                bar(0, 100, 102, 98, 100),
                bar(60, 99, 103, 97, 99),
                bar(120, 104, 105, 101, 104)
        );
        assertEquals(6.0, calculator.process(bars), 1e-9);
    }

    @Test
    void arithmeticMeanIsExposedExplicitly() {
        // Same fixture as above — opt in to the legacy variant directly.
        List<IntradayBar> bars = List.of(
                bar(0, 100, 102, 98, 100),
                bar(60, 99, 103, 97, 99),
                bar(120, 104, 105, 101, 104)
        );
        assertEquals(6.0, calculator.arithmeticMeanTrueRange(bars), 1e-9);
    }

    @Test
    void wilderSmoothingKicksInWhenSeriesLongEnough() {
        // Build 16 bars (15 TRs). Period defaults to 14, so seed = mean of
        // first 14 TRs, then one Wilder step for TR#15.
        // To make the math hand-checkable: every TR is exactly 2.0 except
        // the last, which is 16.0 — that lets us pin both the seed and the
        // evolution arithmetically.
        List<IntradayBar> bars = constantTrBars(/*count*/ 16, /*trueRange*/ 2.0);
        // Mutate the last bar so its TR vs the prior bar is 16.0 instead of 2.0.
        // To make max(high-low, |high-prevClose|, |low-prevClose|) exactly 16,
        // we keep low at prevClose so the low-vs-close arm is zero and the
        // high arm dominates.
        IntradayBar lastSeed = bars.get(bars.size() - 2);
        IntradayBar lastSpike = bar(
                15 * 60,
                lastSeed.close(),                  // open at prior close
                lastSeed.close() + 16,             // high pushes TR to 16
                lastSeed.close(),
                lastSeed.close() + 8
        );
        List<IntradayBar> tweaked = new ArrayList<>(bars);
        tweaked.set(tweaked.size() - 1, lastSpike);

        // Seed = mean of 14 TRs of value 2 = 2.0
        // ATR_15 = (2.0 × 13 + 16.0) / 14 = (26 + 16) / 14 = 42/14 = 3.0
        assertEquals(3.0, calculator.process(tweaked), 1e-9);
    }

    @Test
    void wilderRejectsNonPositivePeriod() {
        List<IntradayBar> bars = constantTrBars(20, 1.0);
        assertThrows(IllegalArgumentException.class, () -> calculator.process(bars, 0));
        assertThrows(IllegalArgumentException.class, () -> calculator.process(bars, -1));
    }

    @Test
    void atrIsNonNegative() {
        List<IntradayBar> bars = List.of(
                bar(0, 100, 101, 99, 100),
                bar(60, 100, 100, 100, 100)
        );
        assertTrue(calculator.process(bars) >= 0.0);
    }

    /**
     * Build {@code count} bars whose TR vs the previous bar is exactly
     * {@code trueRange}. Used to seed the Wilder test with a predictable
     * series.
     */
    private List<IntradayBar> constantTrBars(int count, double trueRange) {
        List<IntradayBar> bars = new ArrayList<>(count);
        double close = 100.0;
        bars.add(bar(0, close, close + 0.5, close - 0.5, close));
        for (int i = 1; i < count; i++) {
            double open = close;
            double high = close + trueRange;        // TR = high - low = trueRange + 0 = trueRange
            double low = close;                      // ensure highLow == trueRange
            double next = close + trueRange / 2.0;
            bars.add(bar(i * 60, open, high, low, next));
            close = next;
        }
        return bars;
    }

    private IntradayBar bar(long offsetSec, double open, double high, double low, double close) {
        return new IntradayBar(
                "X",
                Instant.parse("2026-05-13T13:30:00Z").plusSeconds(offsetSec),
                open, high, low, close, 1_000L, (high + low + close) / 3.0
        );
    }
}
