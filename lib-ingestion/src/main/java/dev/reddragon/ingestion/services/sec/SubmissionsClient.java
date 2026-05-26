package dev.reddragon.ingestion.services.sec;

import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.ingestion.config.SecApiProperties;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;

/**
 * Fetches the submissions JSON document for one company.
 *
 * <p>The submissions endpoint returns the most recent (~1000) filings for
 * a given CIK. The CIK arrives in whichever form the caller has on hand
 * (bare digits, zero-padded, or {@code CIK0000…} prefix); {@link
 * CikFormats#padCik(String)} normalises it before the URL is built, so
 * downstream wiring doesn't need to know which form upstream emits.
 */
public class SubmissionsClient {

    private final SecApiProperties properties;
    private final SecHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SubmissionsClient(SecApiProperties properties, SecHttpClient httpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public SubmissionsClient(SecApiProperties properties, SecHttpClient httpClient) {
        this(properties, httpClient, new ObjectMapper());
    }

    /**
     * Fetch and deserialise the submissions document for {@code cik}.
     *
     * @throws IllegalArgumentException if {@code cik} is null/blank/malformed
     *         (delegated to {@link CikFormats#padCik(String)}).
     */
    public SubmissionsResponse process(String cik) {
        Objects.requireNonNull(cik, "cik is required");
        URI uri = buildSubmissionsUri(cik);
        String json = httpClient.process(uri);
        return deserialise(json);
    }

    private URI buildSubmissionsUri(String cik) {
        String padded = CikFormats.padCik(cik);
        return URI.create(properties.getSubmissionsBaseUrl() + "/CIK" + padded + ".json");
    }

    private SubmissionsResponse deserialise(String json) {
        try {
            return objectMapper.readValue(json, SubmissionsResponse.class);
        } catch (Exception parseError) {
            throw new IllegalStateException("Failed to parse SEC submissions response", parseError);
        }
    }
}
