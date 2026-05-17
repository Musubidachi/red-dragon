package dev.reddragon.ingestion.services.sec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the rate limiter. We test constructor validation and that
 * acquiring fewer tokens than the bucket capacity doesn't block.
 */
class SimpleRateLimiterTest {

    @Test
    void zeroOrNegativeCapacityRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleRateLimiter(0));
        assertThrows(IllegalArgumentException.class, () -> new SimpleRateLimiter(-1));
    }

    @Test
    void acquiringWithinCapacityDoesNotBlock() {
        SimpleRateLimiter limiter = new SimpleRateLimiter(5);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(limiter::process);
        }
        long elapsed = System.currentTimeMillis() - start;

        // Five acquisitions on a 5-token bucket should be nearly instant.
        assertTrue(elapsed < 500, "five acquisitions within capacity should not block; took " + elapsed + "ms");
    }
}
