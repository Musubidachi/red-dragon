package dev.reddragon.ingestion.services.sec;

import java.net.URI;
import java.util.Objects;

import dev.reddragon.ingestion.models.sec.SecFiling;

/**
 * Fetches the primary document body for one SEC filing.
 *
 * <p>This is deliberately a thin wrapper around {@link SecHttpClient}; it
 * reuses the existing SEC user-agent, rate-limit, timeout, and retry policy
 * without wiring filing-body parsing into the candidate pipeline yet.
 */
public class SecFilingBodyClient {

    private final SecHttpClient httpClient;

    public SecFilingBodyClient(SecHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
    }

    public String process(SecFiling filing) {
        Objects.requireNonNull(filing, "filing is required");
        URI primaryDocumentUri = primaryDocumentUri(filing);
        return httpClient.process(primaryDocumentUri);
    }

    private URI primaryDocumentUri(SecFiling filing) {
        if (filing.primaryDocument() == null || filing.primaryDocument().isBlank()) {
            throw new IllegalArgumentException("SEC filing primaryDocument is required for body fetch");
        }
        String primaryDocumentUrl = filing.primaryDocumentUrl();
        if (primaryDocumentUrl == null || primaryDocumentUrl.isBlank()) {
            throw new IllegalArgumentException("SEC filing primaryDocumentUrl could not be built");
        }
        return URI.create(primaryDocumentUrl);
    }
}
