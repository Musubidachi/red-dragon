package dev.reddragon.ingestion.services.sec;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resolves ticker symbols to SEC CIKs using the SEC company_tickers.json file.
 */
public class CikLookupService {

    private static final URI COMPANY_TICKERS_URI = URI.create("https://www.sec.gov/files/company_tickers.json");

    private final SecHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private Map<String, String> tickerToCik;

    public CikLookupService(SecHttpClient httpClient) {
        this(httpClient, new ObjectMapper());
    }

    public CikLookupService(SecHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
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
            String json = httpClient.process(COMPANY_TICKERS_URI);
            JsonNode root = objectMapper.readTree(json);
            Map<String, String> values = new HashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode company = entry.getValue();
                String ticker = company.path("ticker").asText("");
                String cik = company.path("cik_str").asText("");
                if (!ticker.isBlank() && !cik.isBlank()) {
                    values.put(normalize(ticker), cik);
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
