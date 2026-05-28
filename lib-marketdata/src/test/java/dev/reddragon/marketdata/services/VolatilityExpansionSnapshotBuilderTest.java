package dev.reddragon.marketdata.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketVolatilityExpansionSnapshot;

class VolatilityExpansionSnapshotBuilderTest {

    private final VolatilityExpansionSnapshotBuilder builder = new VolatilityExpansionSnapshotBuilder();

    @Test
    void nullInputsRejected() {
        List<IntradayBar> bars = List.of(bar(0, 100.0, 101.0, 99.0, 100.0));

        assertThrows(NullPointerException.class, () -> builder.process(null, bars, bars));
        assertThrows(NullPointerException.class, () -> builder.process("MSFT", null, bars));
        assertThrows(NullPointerException.class, () -> builder.process("MSFT", bars, null));
    }

    @Test
    void zeroBaselineAtrProducesNeutralExpansionAndCompression() {
        List<IntradayBar> currentBars = List.of(
                bar(0, 100.0, 101.0, 99.0, 100.0),
                bar(60, 100.0, 104.0, 98.0, 103.0)
        );
        List<IntradayBar> baselineBars = List.of(
                bar(0, 100.0, 100.0, 100.0, 100.0)
        );

        MarketVolatilityExpansionSnapshot snapshot = builder.process(" msft ", currentBars, baselineBars);

        assertEquals("MSFT", snapshot.symbol());
        assertEquals(0.0, snapshot.baselineAtr(), 1e-9);
        assertEquals(0.50, snapshot.volatilityExpansionScore(), 1e-9);
        assertEquals(0.50, snapshot.volatilityCompressionScore(), 1e-9);
    }

    @Test
    void largeAtrExpansionCapsExpansionScoreAndCompressionFloor() {
        List<IntradayBar> currentBars = List.of(
                bar(0, 100.0, 100.0, 100.0, 100.0),
                bar(60, 105.0, 115.0, 95.0, 110.0)
        );
        List<IntradayBar> baselineBars = List.of(
                bar(0, 100.0, 100.0, 100.0, 100.0),
                bar(60, 100.0, 101.0, 100.0, 100.5)
        );

        MarketVolatilityExpansionSnapshot snapshot = builder.process("MSFT", currentBars, baselineBars);

        assertEquals(20.0, snapshot.currentAtr(), 1e-9);
        assertEquals(1.0, snapshot.baselineAtr(), 1e-9);
        assertEquals(1.0, snapshot.volatilityExpansionScore(), 1e-9);
        assertEquals(0.0, snapshot.volatilityCompressionScore(), 1e-9);
    }

    private static IntradayBar bar(
            long offsetSeconds,
            double open,
            double high,
            double low,
            double close
    ) {
        return new IntradayBar(
                "MSFT",
                Instant.parse("2026-05-08T13:30:00Z").plusSeconds(offsetSeconds),
                open,
                high,
                low,
                close,
                1_000L,
                (high + low + close) / 3.0
        );
    }
}
