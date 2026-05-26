package dev.reddragon.ingestion.services.sec;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token-bucket rate limiter sized for the SEC's per-IP limit.
 *
 * <p>Smoothly releases one permit every {@code 1000 / capacity} ms (e.g. one
 * every 100 ms for the SEC ceiling of 10 req/sec) using a
 * {@link ScheduledExecutorService}. The smooth-release shape stops the
 * "burst at the second boundary" pathology of a refill-all-at-once design,
 * which can exceed the per-IP rate over short windows even when the
 * one-second average is within the cap.
 *
 * <p>This class is intentionally small. It does not handle backoff, retry,
 * or 429 responses — that is the {@link SecHttpClient}'s job.
 *
 * <p>{@link #close()} or {@link #shutdown()} stops the refill scheduler.
 * Callers running inside Spring should rely on the {@code AutoCloseable}
 * contract for orderly shutdown.
 */
public final class SimpleRateLimiter implements AutoCloseable {

    /**
     * Default timeout used by {@link #process()} so a permanently-starved
     * limiter cannot block forever.
     */
    public static final Duration DEFAULT_ACQUIRE_TIMEOUT = Duration.ofSeconds(60);

    private final Semaphore tokens;
    private final int capacity;
    private final ScheduledExecutorService refillScheduler;
    private final AtomicLong releaseCount = new AtomicLong();   // observability + tests

    public SimpleRateLimiter(int requestsPerSecond) {
        if (requestsPerSecond <= 0) {
            throw new IllegalArgumentException("requestsPerSecond must be positive");
        }
        this.capacity = requestsPerSecond;
        this.tokens = new Semaphore(requestsPerSecond);
        this.refillScheduler = Executors.newSingleThreadScheduledExecutor(daemonFactory());
        startRefillScheduler();
    }

    /**
     * Acquire a token, blocking up to {@link #DEFAULT_ACQUIRE_TIMEOUT}.
     *
     * @throws IllegalStateException if interrupted or if the timeout elapses
     *         without acquiring a token (which signals a misconfigured or
     *         deadlocked refiller — better to surface than hang forever)
     */
    public void process() {
        process(DEFAULT_ACQUIRE_TIMEOUT);
    }

    /**
     * Acquire a token, blocking up to the supplied timeout.
     *
     * @throws IllegalStateException if interrupted or if the timeout elapses
     */
    public void process(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive, was: " + timeout);
        }
        try {
            boolean acquired = tokens.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new IllegalStateException(
                        "Timed out after " + timeout + " waiting for SEC rate-limiter token");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate limiter token", interrupted);
        }
    }

    /**
     * Try to acquire a token without blocking; useful for diagnostics and
     * for callers that have their own retry budget.
     *
     * @return {@code true} iff a token was available and consumed
     */
    public boolean tryAcquire() {
        return tokens.tryAcquire();
    }

    /** Current available-permit count. For tests and metrics. */
    public int availablePermits() {
        return tokens.availablePermits();
    }

    /** Bucket capacity (the value passed at construction). */
    public int capacity() {
        return capacity;
    }

    /** Cumulative tokens released by the refill scheduler since construction. */
    public long totalReleased() {
        return releaseCount.get();
    }

    /** Stop the refill scheduler. Implemented via {@link AutoCloseable} so try-with-resources works. */
    @Override
    public void close() {
        shutdown();
    }

    /**
     * Stop the refill scheduler. Idempotent. After calling, {@link #process()}
     * still serves outstanding permits (and any that were already in the
     * bucket) but no new ones are added.
     */
    public void shutdown() {
        refillScheduler.shutdownNow();
    }

    private void startRefillScheduler() {
        long intervalMillis = Math.max(1L, 1000L / capacity);
        refillScheduler.scheduleAtFixedRate(
                this::releaseOne,
                intervalMillis,
                intervalMillis,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Release one permit, clamping at {@link #capacity} so the bucket never
     * exceeds its ceiling. The clamp is critical: without it, an idle limiter
     * accumulates permits indefinitely and a sudden traffic spike could send
     * 60+ requests in the first second.
     */
    private void releaseOne() {
        if (tokens.availablePermits() >= capacity) {
            return;
        }
        tokens.release(1);
        releaseCount.incrementAndGet();
    }

    private static ThreadFactory daemonFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "sec-rate-limiter-refill");
            thread.setDaemon(true);
            return thread;
        };
    }
}
