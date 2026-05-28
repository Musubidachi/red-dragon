package dev.reddragon.app.models;

import java.time.Instant;

public record CalibrationTimestampView(
        String metric,
        Instant observedAt
) {
}
