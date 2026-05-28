package dev.reddragon.ingestion.services.sec;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.ingestion.config.SecApiProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves ticker symbols to SEC CIKs and CIKs back to ticker symbols using
 * the SEC company_tickers.json file.
 *
 * <p>The CIKs returned by this service are <b>zero-padded to 10 digits</b>
 * (the canonical submissions-endpoint form). The SEC payload itself stores
 * the bare {@code cik_str} integer; we normalize it once here via
 * {@link CikFormats#padCik(String)} so downstream callers do not have to
 * remember to pad.
 *
 * <p>The reverse lookup keeps all ticker symbols listed for a CIK, preserving
 * SEC file order. That is deliberate for future CIK-first firehose ingestion:
 * multi-class issuers need share-class-aware emission instead of silently
 * collapsing to one symbol. If a caller needs one representative symbol,
 * {@link #preferredTickerForCik(String)} returns the first SEC-listed ticker.
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
    private Map<String, List<String>> cikToTickers;
    private Instant nextRefreshAt = Instant.EPOCH;

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

    public Optional<String> process(String ticker) {
        return cikForTicker(ticker);
    }

    public synchronized Optional<String> cikForTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        refreshIfNeeded();
        return Optional.ofNullable(tickerToCik.get(normalize(ticker)));
    }

    public synchronized List<String> tickersForCik(String cik) {
        Optional<String> normalizedCik = normalizeCik(cik);
        if (normalizedCik.isEmpty()) {
            return List.of();
        }
        refreshIfNeeded();
        return cikToTickers.getOrDefault(normalizedCik.get(), List.of());
    }

    public Optional<String> preferredTickerForCik(String cik) {
        List<String> tickers = tickersForCik(cik);
        if (tickers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tickers.get(0));
    }

    private void refreshIfNeeded() {
        Instant now = clock.instant();
        if (tickerToCik != null && now.isBefore(nextRefreshAt)) {
            return;
        }

        try {
            Map<String, String> refreshedTickerToCik = loadTickerMap();
            tickerToCik = refreshedTickerToCik;
            cikToTickers = reverseTickerMap(refreshedTickerToCik);
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
            Map<String, String> values = new LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode company = entry.getValue();
                String ticker = company.path("ticker").asText("");
                String rawCik = company.path("cik_str").asText("");
                if (!ticker.isBlank() && CikFormats.looksLikeCik(rawCik)) {
                    values.put(normalize(ticker), CikFormats.padCik(rawCik));
                }
            });
            return Collections.unmodifiableMap(values);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to load SEC company ticker map", error);
        }
    }

    private Map<String, List<String>> reverseTickerMap(Map<String, String> values) {
        Map<String, List<String>> reversed = new HashMap<>();
        values.forEach((ticker, cik) -> reversed
                .computeIfAbsent(cik, ignored -> new ArrayList<>())
                .add(ticker));
        reversed.replaceAll((cik, tickers) -> List.copyOf(tickers));
        return Map.copyOf(reversed);
    }

    private Optional<String> normalizeCik(String cik) {
        if (cik == null || cik.isBlank() || !CikFormats.looksLikeCik(cik)) {
            return Optional.empty();
        }
        return Optional.of(CikFormats.padCik(cik));
    }

    private String normalize(String ticker) {
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
