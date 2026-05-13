package dev.reddragon.ingestion.sec;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Objects;

/**
 * HTTP wrapper that talks to {@code *.sec.gov} with the required headers
 * and stays under the per-IP rate limit.
 *
 * <p>Every other SEC class fetches through this one. Centralising the
 * headers and limiter means there is only one place to look when something
 * about politeness or transport changes.
 */
public class SecHttpClient {

    private final SecApiProperties properties;
    private final SimpleRateLimiter rateLimiter;
    private final RestClient restClient;

    public SecHttpClient(SecApiProperties properties, SimpleRateLimiter rateLimiter, RestClient restClient) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.restClient = restClient;
    }

    public SecHttpClient(SecApiProperties properties, SimpleRateLimiter rateLimiter) {
        this(properties, rateLimiter, RestClient.create());
    }

    /**
     * Fetch the body of {@code uri} as a string, blocking on the rate
     * limiter first.
     *
     * <p>This is the one entry point exposed by this class.
     */
    public String process(URI uri) {
        Objects.requireNonNull(uri, "uri is required");
        waitForRateLimiterToken();
        return fetchBody(uri);
    }

    private void waitForRateLimiterToken() {
        rateLimiter.process();
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
