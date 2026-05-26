package dev.reddragon.ingestion.services.sec;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.ingestion.config.SecApiProperties;

/**
 * Resolves ticker symbols to SEC CIKs using the SEC company_tickers.json file.
 *
 * <p>The CIKs returned by this service are <b>zero-padded to 10 digits</b>
 * (the canonical submissions-endpoint form). The SEC payload itself stores
 * the bare {@code cik_str} integer; we normalise it once here via
 * {@link CikFormats#padCik(String)} so downstream callers don't have to
 * remember to pad. See lib-ingestion REVIEW.md Finding #12.
 *
 * <p>The source URL is read from {@link SecApiProperties#getCompanyTickersUrl()}
 * (Finding #10) so integration tests can stub it and so a future deployment
 * can point at a mirror or local cache.
 */
public class CikLookupService {

    private final SecApiProperties properties;
    private final SecHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private Map<String, String> tickerToCik;

    public CikLookupService(SecApiProperties properties, SecHttpClient httpClient) {
        this(properties, httpClient, new ObjectMapper());
    }

    public CikLookupService(SecApiProperties properties, SecHttpClient httpClient, ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties, "properties is required");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
    }

    public synchronized Optional<String> process(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        if (tickerToCik == null) {
            tickerToCik = loadTickerMap();
        }
        return Optional.ofNullable(tickerToCik.get(normalize(ticker)));
    }

    private Map<String, String> loadTickerMap() {
        try {
            URI source = URI.create(properties.getCompanyTickersUrl());
            String json = httpClient.process(source);
            JsonNode root = objectMapper.readTree(json);
            Map<String, String> values = new HashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode company = entry.getValue();
                String ticker = company.path("ticker").asText("");
                String rawCik = company.path("cik_str").asText("");
                if (!ticker.isBlank() && CikFormats.looksLikeCik(rawCik)) {
                    // Normalise to 10-digit zero-padded form (REVIEW.md #12).
                    values.put(normalize(ticker), CikFormats.padCik(rawCik));
                }
            });
            return Map.copyOf(values);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to load SEC company ticker map", error);
        }
    }

    private String normalize(String ticker) {
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
