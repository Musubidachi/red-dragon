package dev.reddragon.marketdata.services;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketIntradayStructureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntradayStructureSnapshotBuilderTest {

    private final IntradayStructureSnapshotBuilder builder = new IntradayStructureSnapshotBuilder();

    @Test
    void nullBarsRejected() {
        assertThrows(NullPointerException.class, () -> builder.process(null));
    }

    @Test
    void emptyBarsRejected() {
        assertThrows(IllegalArgumentException.class, () -> builder.process(List.of()));
    }

    @Test
    void buildsSnapshotFromValidBars() {
        List<IntradayBar> bars = List.of(
                bar(0, 100, 101, 99, 100, 1_000L),
                bar(60, 100, 102, 99, 101, 1_500L),
                bar(120, 101, 103, 100, 102, 2_000L)
        );

        MarketIntradayStructureSnapshot snapshot = builder.process(bars);
        assertNotNull(snapshot);
        // All score fields should be within their documented ranges
        assertTrue(snapshot.rotationalQualityScore() >= 0 && snapshot.rotationalQualityScore() <= 1);
        assertTrue(snapshot.directionalPersistenceScore() >= 0 && snapshot.directionalPersistenceScore() <= 1);
        assertTrue(snapshot.intradayTrendStrength() >= 0 && snapshot.intradayTrendStrength() <= 1);
        assertTrue(snapshot.vwapReclaimStrength() >= 0 && snapshot.vwapReclaimStrength() <= 1);
    }

    @Test
    void closeAboveVwapMarksAboveVwapTrue() {
        // Latest close is well above VWAP
        List<IntradayBar> bars = List.of(
                bar(0,  100, 100, 100, 100, 1_000L),
                bar(60, 110, 110, 110, 110, 1_000L),
                bar(120, 120, 120, 120, 120, 1_000L)
        );
        MarketIntradayStructureSnapshot snapshot = builder.process(bars);
        assertEquals(true, snapshot.aboveVwap());
    }

    @Test
    void vwapUsesLatestSessionWhenBarsSpanSessions() {
        List<IntradayBar> bars = List.of(
                barAt("2026-05-13T13:30:00Z", 200, 202, 198, 200, 1_000L),
                barAt("2026-05-14T13:30:00Z", 100, 102, 98, 100, 1_000L),
                barAt("2026-05-14T13:31:00Z", 110, 112, 108, 110, 3_000L)
        );

        MarketIntradayStructureSnapshot snapshot = builder.process(bars);

        assertEquals(107.5, snapshot.sessionVwap(), 1e-9);
    }

    private IntradayBar bar(long offsetSec, double open, double high, double low, double close, long volume) {
        return barAt(
                Instant.parse("2026-05-13T13:30:00Z").plusSeconds(offsetSec),
                open,
                high,
                low,
                close,
                volume
        );
    }

    private IntradayBar barAt(String startTime, double open, double high, double low, double close, long volume) {
        return barAt(Instant.parse(startTime), open, high, low, close, volume);
    }

    private IntradayBar barAt(Instant startTime, double open, double high, double low, double close, long volume) {
        return new IntradayBar(
                "X",
                startTime,
                open, high, low, close, volume, (high + low + close) / 3.0
        );
    }
}
