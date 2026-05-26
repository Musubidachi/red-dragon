package dev.reddragon.ingestion.services.sec;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import dev.reddragon.ingestion.config.SecApiProperties;

/**
 * HTTP wrapper that talks to {@code *.sec.gov} with the required headers
 * and stays under the per-IP rate limit.
 *
 * <p>Every other SEC class fetches through this one. Centralising the
 * headers, limiter, timeouts, and retry policy means there is only one
 * place to look when something about politeness or transport changes.
 *
 * <p>Retry behaviour: on a {@code 429 Too Many Requests} or any {@code 5xx},
 * the request is retried with jittered exponential backoff (base + factor
 * 2 each attempt, capped at {@code maxBackoffMillis}, plus 0–25% jitter)
 * up to {@code maxRetries} times. Each retry re-acquires a rate-limiter
 * token so the per-IP ceiling is honoured during recovery. {@code 4xx}
 * responses other than 429 propagate immediately — client errors are not
 * recoverable by waiting.
 */
public class SecHttpClient {

    private final SecApiProperties properties;
    private final SimpleRateLimiter rateLimiter;
    private final RestClient restClient;

    public SecHttpClient(SecApiProperties properties, SimpleRateLimiter rateLimiter, RestClient restClient) {
        this.properties = Objects.requireNonNull(properties, "properties is required");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter is required");
        this.restClient = Objects.requireNonNull(restClient, "restClient is required");
    }

    public SecHttpClient(SecApiProperties properties, SimpleRateLimiter rateLimiter) {
        this(properties, rateLimiter, defaultRestClient(properties));
    }

    private static RestClient defaultRestClient(SecApiProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMillis()));
        // Note: JdkClientHttpRequestFactory does not expose a separate connect
        // timeout; the JDK HttpClient applies its own connect timeout via the
        // builder. For now both phases share the read-timeout ceiling — if
        // connect-time hangs become an operational concern, swap to a
        // ClientHttpRequestFactory that supports an explicit connect timeout.
        return RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Fetch the body of {@code uri} as a string, blocking on the rate
     * limiter first.
     *
     * <p>This is the one entry point exposed by this class.
     */
    public String process(URI uri) {
        Objects.requireNonNull(uri, "uri is required");
        return fetchWithRetry(uri);
    }

    private String fetchWithRetry(URI uri) {
        RestClientException last = null;
        int attempts = properties.getMaxRetries() + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            rateLimiter.process();
            try {
                return fetchBody(uri);
            } catch (HttpStatusCodeException error) {
                if (!isRetryable(error.getStatusCode()) || attempt == attempts) {
                    throw error;
                }
                last = error;
                sleepBackoff(attempt);
            } catch (RestClientException error) {
                // I/O failures (connect refused, read timeout, etc.) — retry
                // like a 5xx because they're transient.
                if (attempt == attempts) {
                    throw error;
                }
                last = error;
                sleepBackoff(attempt);
            }
        }
        throw new IllegalStateException(
                "SEC HTTP fetch exhausted retries for " + uri, last);
    }

    private boolean isRetryable(HttpStatusCode status) {
        if (status == null) {
            return false;
        }
        if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return true;
        }
        return status.is5xxServerError();
    }

    /**
     * Jittered exponential backoff: {@code base * 2^(attempt-1)} milliseconds
     * capped at {@code maxBackoffMillis}, plus a uniform jitter in
     * {@code [0, 25%)} of the computed delay so concurrent retriers do not
     * march in lockstep.
     */
    private void sleepBackoff(int attempt) {
        long base = properties.getBackoffBaseMillis();
        long max = properties.getMaxBackoffMillis();
        long delay = base * (1L << Math.min(attempt - 1, 16));   // cap shift to avoid overflow
        if (delay <= 0L || delay > max) {
            delay = max;
        }
        long jitter = ThreadLocalRandom.current().nextLong(delay / 4 + 1);
        long sleepMs = delay + jitter;
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during SEC HTTP retry backoff", interrupted);
        }
    }

    private String fetchBody(URI uri) {
        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .retrieve()
                .body(String.class);
    }
}
