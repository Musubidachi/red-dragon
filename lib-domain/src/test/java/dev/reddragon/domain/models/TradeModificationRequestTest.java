package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeModificationRequestTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-13T00:00:00Z");

    @Test
    void createdAtIsRequired() {
        assertThrows(NullPointerException.class, () -> new TradeModificationRequest(
                "id", "ABC", null, TradeModificationAction.HOLD, 0.5, List.of()));
    }

    @Test
    void severityStillClampsAfterExplicitTimestamp() {
        TradeModificationRequest request = new TradeModificationRequest(
                "id", "ABC", CREATED_AT, TradeModificationAction.HOLD, 2.0, List.of("reason"));

        assertEquals(CREATED_AT, request.createdAt());
        assertEquals(1.0, request.severity());
    }
}
