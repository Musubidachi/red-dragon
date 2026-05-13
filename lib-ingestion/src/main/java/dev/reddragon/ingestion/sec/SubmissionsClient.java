package dev.reddragon.ingestion.sec;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Objects;

/**
 * Fetches the submissions JSON document for one company.
 *
 * <p>The submissions endpoint returns the most recent (~1000) filings for
 * a given CIK. The CIK is supplied unpadded; this class zero-pads it to
 * the 10 characters the SEC URL expects.
 */
public class SubmissionsClient {

    private final SecApiProperties properties;
    private final SecHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SubmissionsClient(SecApiProperties properties, SecHttpClient httpClient) {
        this(properties, httpClient, new ObjectMapper());
    }

    /**
     * Fetch and deserialise the submissions document for {@code cik}.
     */
    public SubmissionsResponse process(String cik) {
        Objects.requireNonNull(cik, "cik is required");
        URI uri = buildSubmissionsUri(cik);
        String json = httpClient.process(uri);
        return deserialise(json);
    }

    private URI buildSubmissionsUri(String cik) {
        String padded = zeroPadCik(cik);
        return URI.create(properties.getSubmissionsBaseUrl() + "/CIK" + padded + ".json");
    }

    private String zeroPadCik(String cik) {
        long numeric = Long.parseLong(cik.trim());
        return String.format("%010d", numeric);
    }

    private SubmissionsResponse deserialise(String json) {
        try {
            return objectMapper.readValue(json, SubmissionsResponse.class);
        } catch (Exception parseError) {
            throw new IllegalStateException("Failed to parse SEC submissions response", parseError);
        }
    }
}
