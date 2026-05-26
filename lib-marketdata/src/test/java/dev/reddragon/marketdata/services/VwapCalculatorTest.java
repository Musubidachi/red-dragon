package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VwapCalculatorTest {

    private final VwapCalculator calculator = new VwapCalculator();

    @Test
    void nullBarsRejected() {
        assertThrows(NullPointerException.class, () -> calculator.process(null));
    }

    @Test
    void emptyBarsReturnsZero() {
        assertEquals(0.0, calculator.process(List.of()));
    }

    @Test
    void zeroVolumeReturnsZero() {
        IntradayBar zeroVol = new IntradayBar("X", Instant.parse("2026-05-13T13:30:00Z"),
                100, 101, 99, 100, 0L, 100.0);
        assertEquals(0.0, calculator.process(List.of(zeroVol)));
    }

    @Test
    void vwapEqualsTypicalPriceForOneBar() {
        // typical = (H+L+C)/3 = (102 + 98 + 100) / 3 = 100
        IntradayBar bar = new IntradayBar("X", Instant.parse("2026-05-13T13:30:00Z"),
                100, 102, 98, 100, 1_000L, 100.0);
        assertEquals(100.0, calculator.process(List.of(bar)), 1e-9);
    }

    @Test
    void vwapIsWeightedAverageAcrossBars() {
        // bar1: typical=100, vol=1000 -> contributes 100,000
        // bar2: typical=110, vol=3000 -> contributes 330,000
        // VWAP = (100,000 + 330,000) / 4000 = 107.5
        IntradayBar a = new IntradayBar("X", Instant.parse("2026-05-13T13:30:00Z"),
                100, 102, 98, 100, 1_000L, 100.0);
        IntradayBar b = new IntradayBar("X", Instant.parse("2026-05-13T13:31:00Z"),
                110, 112, 108, 110, 3_000L, 110.0);
        assertEquals(107.5, calculator.process(List.of(a, b)), 1e-9);
    }
}
