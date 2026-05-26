package dev.reddragon.ingestion.services.sec;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.ingestion.config.SecApiProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves ticker symbols to SEC CIKs using the SEC company_tickers.json file.
 *
 * <p>The CIKs returned by this service are <b>zero-padded to 10 digits</b>
 * (the canonical submissions-endpoint form). The SEC payload itself stores
 * the bare {@code cik_str} integer; we normalize it once here via
 * {@link CikFormats#padCik(String)} so downstream callers do not have to
 * remember to pad.
 *
 * <p>The source URL and refresh TTL are read from {@link SecApiProperties} so
 * tests can stub the endpoint and operators can point at a mirror or local
 * cache. After the first successful load, refresh failures fail stale: the
 * previous map remains usable and the next refresh attempt is delayed by the
 * configured TTL.
 */
@Slf4j
public class CikLookupService {

    private final SecApiProperties properties;
    private final SecHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private Map<String, String> tickerToCik;
    private Instant nextRefreshAt = Instant.EPOCH;

    public CikLookupService(SecApiProperties properties, SecHttpClient httpClient) {
        this(properties, httpClient, new ObjectMapper(), Clock.systemUTC());
    }

    public CikLookupService(SecApiProperties properties, SecHttpClient httpClient, ObjectMapper objectMapper) {
        this(properties, httpClient, objectMapper, Clock.systemUTC());
    }

    public CikLookupService(
            SecApiProperties properties,
            SecHttpClient httpClient,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "properties is required");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public synchronized Optional<String> process(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        refreshIfNeeded();
        return Optional.ofNullable(tickerToCik.get(normalize(ticker)));
    }

    private void refreshIfNeeded() {
        Instant now = clock.instant();
        if (tickerToCik != null && now.isBefore(nextRefreshAt)) {
            return;
        }

        try {
            tickerToCik = loadTickerMap();
            scheduleNextRefresh();
        } catch (RuntimeException error) {
            if (tickerToCik == null) {
                throw error;
            }
            scheduleNextRefresh();
            log.warn(
                    "Failed to refresh SEC company ticker map; using cached map until {}",
                    nextRefreshAt,
                    error
            );
        }
    }

    private void scheduleNextRefresh() {
        nextRefreshAt = clock.instant().plus(refreshTtl());
    }

    private Duration refreshTtl() {
        return Duration.ofMillis(properties.getCompanyTickersTtlMillis());
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
