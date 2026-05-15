package dev.reddragon.app.models;

import java.time.Instant;

public record CalibrationOutcomeView(
        String candidateId,
        String symbol,
        Instant observedAt,
        double realizedReturn,
        double maxDrawdown,
        int daysHeld,
        boolean thesisWorked
) {
}
