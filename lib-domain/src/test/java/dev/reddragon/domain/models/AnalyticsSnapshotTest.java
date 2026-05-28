package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyticsSnapshotTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-05-13T00:00:00Z");

    @Test
    void observedAtIsRequired() {
        assertThrows(NullPointerException.class, () -> new AnalyticsSnapshot(
                "id", "ABC", null, RegimeLabel.MIXED,
                0.6, 0.7, 0.8, 0.5, 0.4, List.of()));
    }

    @Test
    void dominantScoreReturnsScoreDimension() {
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot(
                "id", "ABC", OBSERVED_AT, RegimeLabel.MIXED,
                0.6, 0.7, 0.8, 0.5, 0.9, List.of());

        assertEquals(ScoreDimension.DEPLOYMENT_CONFIDENCE, snapshot.dominantScore());
    }
}
