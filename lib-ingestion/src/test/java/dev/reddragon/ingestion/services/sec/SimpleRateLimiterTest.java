package dev.reddragon.ingestion.services.sec;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Smoke tests for the rate limiter. We test constructor validation, capacity
 * behaviour, the new tryAcquire / timeout APIs, and the scheduler refill +
 * clamp invariants.
 */
class SimpleRateLimiterTest {

    @Test
    void zeroOrNegativeCapacityRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleRateLimiter(0));
        assertThrows(IllegalArgumentException.class, () -> new SimpleRateLimiter(-1));
    }

    @Test
    void acquiringWithinCapacityDoesNotBlock() throws Exception {
        try (SimpleRateLimiter limiter = new SimpleRateLimiter(5)) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 5; i++) {
                assertDoesNotThrow((Executable) limiter::process);
            }
            long elapsed = System.currentTimeMillis() - start;

            // Five acquisitions on a 5-token bucket should be nearly instant.
            assertTrue(elapsed < 500, "five acquisitions within capacity should not block; took " + elapsed + "ms");
        }
    }

    @Test
    void processWithZeroOrNegativeTimeoutRejected() throws Exception {
        try (SimpleRateLimiter limiter = new SimpleRateLimiter(5)) {
            assertThrows(IllegalArgumentException.class, () -> limiter.process(Duration.ZERO));
            assertThrows(IllegalArgumentException.class, () -> limiter.process(Duration.ofMillis(-1)));
            assertThrows(IllegalArgumentException.class, () -> limiter.process(null));
        }
    }

    @Test
    void processTimesOutWhenStarved() throws Exception {
        // Capacity 1, drain it, then ask for another with a 50ms budget — the
        // refill scheduler ticks every 1000/1 = 1000ms so the second acquire
        // must time out at 50ms.
        try (SimpleRateLimiter limiter = new SimpleRateLimiter(1)) {
            limiter.process();
            long start = System.currentTimeMillis();
            assertThrows(IllegalStateException.class,
                    () -> limiter.process(Duration.ofMillis(50)));
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed >= 40,
                    "timeout should observe roughly its budget; elapsed=" + elapsed + "ms");
            assertTrue(elapsed < 500,
                    "timeout should not far exceed its budget; elapsed=" + elapsed + "ms");
        }
    }

    @Test
    void tryAcquireReturnsFalseWhenEmpty() throws Exception {
        try (SimpleRateLimiter limiter = new SimpleRateLimiter(1)) {
            assertTrue(limiter.tryAcquire(), "first acquire should succeed");
            assertFalse(limiter.tryAcquire(), "second acquire should fail; bucket is empty");
        }
    }

    @Test
    void availablePermitsClampedAtCapacityEvenAfterLongIdle() throws Exception {
        // The scheduler releases one permit per 1000/capacity ms but must
        // never exceed the bucket capacity. Build a small bucket, wait long
        // enough that an unclamped scheduler would have fired multiple
        // refills, and confirm permits stay at capacity.
        try (SimpleRateLimiter limiter = new SimpleRateLimiter(2)) {
            // 1000/2 = 500ms refill interval. Wait 1.5s — three intervals.
            Thread.sleep(1_500);
            assertEquals(limiter.capacity(), limiter.availablePermits(),
                    "idle bucket must clamp at capacity, not accumulate");
        }
    }

    @Test
    void capacityAndTotalReleasedExposed() throws Exception {
        try (SimpleRateLimiter limiter = new SimpleRateLimiter(3)) {
            assertEquals(3, limiter.capacity());
            assertTrue(limiter.totalReleased() >= 0);
        }
    }
}
