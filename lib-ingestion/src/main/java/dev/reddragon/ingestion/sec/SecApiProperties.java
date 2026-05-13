package dev.reddragon.ingestion.sec;

/**
 * Configuration values needed to talk to the SEC EDGAR HTTP API.
 *
 * <p>Why this class exists: every SEC request must carry a real
 * {@code User-Agent} with a contact email, must stay under 10 requests per
 * second per IP, and must point at the right base host. Centralising those
 * values here means every SEC class reads from one place and the operator
 * sets them once.
 */
public class SecApiProperties {

    /** The exact string sent in the {@code User-Agent} HTTP header. */
    private final String userAgent;

    /** Base URL for the submissions JSON API. */
    private final String submissionsBaseUrl;

    /** Maximum HTTP requests per second to send to {@code *.sec.gov}. */
    private final int requestsPerSecond;

    public SecApiProperties(String userAgent, String submissionsBaseUrl, int requestsPerSecond) {
        this.userAgent = userAgent;
        this.submissionsBaseUrl = submissionsBaseUrl;
        this.requestsPerSecond = requestsPerSecond;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getSubmissionsBaseUrl() {
        return submissionsBaseUrl;
    }

    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }

    /**
     * Returns a conservative default suitable for local development. The
     * caller must override {@code userAgent} with a real contact address
     * before any production use; the SEC will reject requests that look
     * like an unidentified script.
     */
    public static SecApiProperties defaults() {
        return new SecApiProperties(
                "red-dragon (replace-me@example.invalid)",
                "https://data.sec.gov/submissions",
                5
        );
    }
}
