package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketQuoteTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-05-13T00:00:00Z");

    @Test
    void observedAtIsRequired() {
        assertThrows(NullPointerException.class, () -> new MarketQuote(
                "ABC", null, 10.0, 9.9, 10.1, 100L, MarketDataQuality.COMPLETE, List.of()));
    }

    @Test
    void unavailableCanUseExplicitObservedAt() {
        MarketQuote quote = MarketQuote.unavailable(" abc ", OBSERVED_AT, "missing");

        assertEquals("ABC", quote.symbol());
        assertEquals(OBSERVED_AT, quote.observedAt());
        assertEquals(MarketDataQuality.EMPTY_BARS, quote.quality());
        assertEquals(List.of("missing"), quote.notes());
        assertFalse(quote.available());
    }
}
