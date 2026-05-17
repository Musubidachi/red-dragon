package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
    void atrIsAverageOfTrueRangesAcrossBars() {
        // bar1: H=102 L=98 close=100
        // bar2: H=103 L=97 close=99 -> TR = max(6, |103-100|, |97-100|) = 6
        // bar3: H=105 L=101 close=104 -> TR = max(4, |105-99|=6, |101-99|=2) = 6
        // ATR = (6 + 6) / 2 = 6
        List<IntradayBar> bars = List.of(
                bar(0, 100, 102, 98, 100),
                bar(60, 99, 103, 97, 99),
                bar(120, 104, 105, 101, 104)
        );
        assertEquals(6.0, calculator.process(bars), 1e-9);
    }

    @Test
    void atrIsNonNegative() {
        // Random-ish bars; result must still be >= 0
        List<IntradayBar> bars = List.of(
                bar(0, 100, 101, 99, 100),
                bar(60, 100, 100, 100, 100)
        );
        assertTrue(calculator.process(bars) >= 0.0);
    }

    private IntradayBar bar(long offsetSec, double open, double high, double low, double close) {
        return new IntradayBar(
                "X",
                Instant.parse("2026-05-13T13:30:00Z").plusSeconds(offsetSec),
                open, high, low, close, 1_000L, (high + low + close) / 3.0
        );
    }
}
