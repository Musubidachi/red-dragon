package dev.reddragon.ingestion.services.sec;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Token-bucket rate limiter sized for the SEC's per-IP limit.
 *
 * <p>Every call to {@link #process()} blocks until a token is available, then
 * returns. The bucket refills on a fixed schedule by a single daemon thread.
 *
 * <p>This class is intentionally small. It does not handle backoff, retry,
 * or 429 responses — that is the {@link SecHttpClient}'s job.
 */
public final class SimpleRateLimiter {

    private final Semaphore tokens;
    private final int capacity;

    public SimpleRateLimiter(int requestsPerSecond) {
        if (requestsPerSecond <= 0) {
            throw new IllegalArgumentException("requestsPerSecond must be positive");
        }
        this.capacity = requestsPerSecond;
        this.tokens = new Semaphore(requestsPerSecond);
        startRefillThread();
    }

    /**
     * Block until a token is available, then consume it.
     *
     * <p>This is the one public action the class performs. Naming it
     * {@code process} keeps the convention consistent across the codebase
     * even though the work performed is "wait and consume one token."
     */
    public void process() {
        try {
            tokens.acquire();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate limiter token", interrupted);
        }
    }

    private void startRefillThread() {
        Thread refill = new Thread(this::refillForever, "sec-rate-limiter-refill");
        refill.setDaemon(true);
        refill.start();
    }

    private void refillForever() {
        while (!Thread.currentThread().isInterrupted()) {
            sleepOneSecond();
            replenishBucket();
        }
    }

    private void sleepOneSecond() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void replenishBucket() {
        int available = tokens.availablePermits();
        int missing = capacity - available;
        if (missing > 0) {
            tokens.release(missing);
        }
    }
}
