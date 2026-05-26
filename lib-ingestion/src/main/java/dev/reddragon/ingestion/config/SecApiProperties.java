package dev.reddragon.ingestion.config;

import java.time.Duration;

/**
 * Configuration values needed to talk to the SEC EDGAR HTTP API.
 *
 * <p>Why this class exists: every SEC request must carry a real
 * {@code User-Agent} with a contact email, must stay under 10 requests per
 * second per IP, and must point at the right base host. Centralizing those
 * values here means every SEC class reads from one place and the operator
 * sets them once.
 */
public class SecApiProperties {

    /** Default TCP connect timeout; keeps SEC connection attempts bounded. */
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000;

    /** Default response read timeout; covers slow SEC responses without wedging the poller. */
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 30_000;

    /** Default URL of the SEC company ticker to CIK map, hosted by www.sec.gov. */
    public static final String DEFAULT_COMPANY_TICKERS_URL = "https://www.sec.gov/files/company_tickers.json";

    /** Default refresh interval for the SEC company ticker map. */
    public static final long DEFAULT_COMPANY_TICKERS_TTL_MILLIS = 604_800_000L;

    /** Default 429/5xx retry-loop ceiling. */
    public static final int DEFAULT_MAX_RETRIES = 5;

    /** Default base backoff between retries, doubled each attempt with jitter. */
    public static final long DEFAULT_BACKOFF_BASE_MILLIS = 500L;

    /** Default backoff ceiling; a single retry never sleeps longer than this. */
    public static final long DEFAULT_MAX_BACKOFF_MILLIS = 30_000L;

    /** The exact string sent in the {@code User-Agent} HTTP header. */
    private final String userAgent;

    /** Base URL for the submissions JSON API. */
    private final String submissionsBaseUrl;

    /**
     * Full URL of the SEC company-tickers JSON document used by
     * {@code CikLookupService}. Lives on {@code www.sec.gov}, not the
     * submissions host. It is configurable so integration tests can stub it
     * and future deployments can point at a mirror or local cache.
     */
    private final String companyTickersUrl;

    /** Cache TTL for {@code company_tickers.json}. */
    private final long companyTickersTtlMillis;

    /** Maximum HTTP requests per second to send to {@code *.sec.gov}. */
    private final int requestsPerSecond;

    /** TCP connect timeout in milliseconds; bounds the time we wait to open a socket. */
    private final int connectTimeoutMillis;

    /** Response read timeout in milliseconds; bounds the time we wait for body bytes. */
    private final int readTimeoutMillis;

    /** Maximum number of retry attempts on 429/5xx before giving up. */
    private final int maxRetries;

    /** Base sleep in milliseconds for exponential backoff between retries. */
    private final long backoffBaseMillis;

    /** Upper bound in milliseconds on any individual retry sleep. */
    private final long maxBackoffMillis;

    /**
     * Three-arg constructor preserved for backwards-compatible wiring. Uses
     * the {@code DEFAULT_*} constants for the transport, retry, companion URL,
     * and companion cache knobs.
     */
    public SecApiProperties(String userAgent, String submissionsBaseUrl, int requestsPerSecond) {
        this(userAgent, submissionsBaseUrl, DEFAULT_COMPANY_TICKERS_URL, requestsPerSecond,
                DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS,
                DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_BASE_MILLIS, DEFAULT_MAX_BACKOFF_MILLIS,
                DEFAULT_COMPANY_TICKERS_TTL_MILLIS);
    }

    public SecApiProperties(
            String userAgent,
            String submissionsBaseUrl,
            String companyTickersUrl,
            int requestsPerSecond,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            int maxRetries,
            long backoffBaseMillis,
            long maxBackoffMillis
    ) {
        this(userAgent, submissionsBaseUrl, companyTickersUrl, requestsPerSecond,
                connectTimeoutMillis, readTimeoutMillis, maxRetries,
                backoffBaseMillis, maxBackoffMillis, DEFAULT_COMPANY_TICKERS_TTL_MILLIS);
    }

    public SecApiProperties(
            String userAgent,
            String submissionsBaseUrl,
            String companyTickersUrl,
            int requestsPerSecond,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            int maxRetries,
            long backoffBaseMillis,
            long maxBackoffMillis,
            long companyTickersTtlMillis
    ) {
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("userAgent is required");
        }
        if (looksLikePlaceholder(userAgent)) {
            throw new IllegalArgumentException(
                    "userAgent appears to be a placeholder (\"" + userAgent + "\"). "
                            + "SEC fair-access policy requires a real contact email; set "
                            + "red-dragon.sec.user-agent to something like "
                            + "\"red-dragon (you@example-real.com)\" before starting the service.");
        }
        if (submissionsBaseUrl == null || submissionsBaseUrl.isBlank()) {
            throw new IllegalArgumentException("submissionsBaseUrl is required");
        }
        if (companyTickersUrl == null || companyTickersUrl.isBlank()) {
            throw new IllegalArgumentException("companyTickersUrl is required");
        }
        if (requestsPerSecond <= 0 || requestsPerSecond > 10) {
            throw new IllegalArgumentException(
                    "requestsPerSecond must be between 1 and 10 (SEC fair-access ceiling), was: "
                            + requestsPerSecond);
        }
        if (connectTimeoutMillis <= 0 || connectTimeoutMillis > 60_000) {
            throw new IllegalArgumentException(
                    "connectTimeoutMillis must be between 1 and 60000, was: " + connectTimeoutMillis);
        }
        if (readTimeoutMillis <= 0 || readTimeoutMillis > 300_000) {
            throw new IllegalArgumentException(
                    "readTimeoutMillis must be between 1 and 300000, was: " + readTimeoutMillis);
        }
        if (maxRetries < 0 || maxRetries > 10) {
            throw new IllegalArgumentException(
                    "maxRetries must be between 0 and 10, was: " + maxRetries);
        }
        if (backoffBaseMillis <= 0 || backoffBaseMillis > 10_000) {
            throw new IllegalArgumentException(
                    "backoffBaseMillis must be between 1 and 10000, was: " + backoffBaseMillis);
        }
        if (maxBackoffMillis < backoffBaseMillis) {
            throw new IllegalArgumentException(
                    "maxBackoffMillis (" + maxBackoffMillis + ") cannot be less than backoffBaseMillis ("
                            + backoffBaseMillis + ")");
        }
        if (companyTickersTtlMillis <= 0 || companyTickersTtlMillis > Duration.ofDays(30).toMillis()) {
            throw new IllegalArgumentException(
                    "companyTickersTtlMillis must be between 1 and 2592000000, was: "
                            + companyTickersTtlMillis);
        }
        this.userAgent = userAgent;
        this.submissionsBaseUrl = submissionsBaseUrl;
        this.companyTickersUrl = companyTickersUrl;
        this.companyTickersTtlMillis = companyTickersTtlMillis;
        this.requestsPerSecond = requestsPerSecond;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.maxRetries = maxRetries;
        this.backoffBaseMillis = backoffBaseMillis;
        this.maxBackoffMillis = maxBackoffMillis;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getSubmissionsBaseUrl() {
        return submissionsBaseUrl;
    }

    public String getCompanyTickersUrl() {
        return companyTickersUrl;
    }

    public long getCompanyTickersTtlMillis() {
        return companyTickersTtlMillis;
    }

    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getBackoffBaseMillis() {
        return backoffBaseMillis;
    }

    public long getMaxBackoffMillis() {
        return maxBackoffMillis;
    }

    /**
     * Heuristic detector for placeholder User-Agent strings. SEC fair-access
     * policy forbids generic UAs such as fake example domains or no-reply
     * addresses and may block repeat offenders.
     */
    private static boolean looksLikePlaceholder(String userAgent) {
        String lower = userAgent.toLowerCase();
        return lower.contains("replace-me")
                || lower.contains("example.invalid")
                || lower.contains("example.com")
                || lower.contains("example.org")
                || lower.contains("example.net")
                || lower.contains("noreply@")
                || lower.contains("no-reply@");
    }
}
