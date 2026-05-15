package dev.reddragon.marketdata.services.provider.schwab;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.marketdata.config.SchwabMarketDataProperties;
import dev.reddragon.marketdata.models.MarketBar;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;

public class SchwabMarketDataProvider implements MarketDataProvider {

    private final SchwabMarketDataProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SchwabAccessTokenSupplier tokenSupplier;
    private final Map<String, SchwabBarCacheEntry> cache = new ConcurrentHashMap<>();

    public SchwabMarketDataProvider(SchwabMarketDataProperties properties) {
        this(properties, RestClient.create(), new ObjectMapper(), new StaticSchwabAccessTokenSupplier(properties));
    }

    public SchwabMarketDataProvider(
            SchwabMarketDataProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this(properties, restClient, objectMapper, new StaticSchwabAccessTokenSupplier(properties));
    }

    /**
     * Full constructor. Pass a custom {@link SchwabAccessTokenSupplier} when
     * OAuth-backed token rotation is in play; otherwise use the simpler
     * constructors above and the supplier falls back to the static token
     * baked into {@code properties}.
     */
    public SchwabMarketDataProvider(
            SchwabMarketDataProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper,
            SchwabAccessTokenSupplier tokenSupplier
    ) {
        this.properties = Objects.requireNonNull(properties, "properties is required");
        this.restClient = Objects.requireNonNull(restClient, "restClient is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.tokenSupplier = Objects.requireNonNull(tokenSupplier, "tokenSupplier is required");
    }

    @Override
    public List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to) {
        validate(symbol, from, to);
        if (!properties.configured()) {
            return List.of();
        }

        String cacheKey = cacheKey(symbol, from, to);
        SchwabBarCacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.expired(properties.getCacheTtlSeconds())) {
            return cached.bars();
        }

        List<MarketBar> bars = fetchBarsWithRetry(symbol, from, to);
        cache.put(cacheKey, new SchwabBarCacheEntry(Instant.now(), bars));
        return bars;
    }

    private void validate(String symbol, LocalDate from, LocalDate to) {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        if (from == null || to == null) throw new IllegalArgumentException("from and to dates are required");
        if (to.isBefore(from)) throw new IllegalArgumentException("to date cannot be before from date");
    }

    private String cacheKey(String symbol, LocalDate from, LocalDate to) {
        return symbol.trim().toUpperCase() + ":" + from + ":" + to;
    }

    private List<MarketBar> fetchBarsWithRetry(String symbol, LocalDate from, LocalDate to) {
        RuntimeException last = null;
        int attempts = Math.max(1, properties.getMaxRetries() + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return fetchBars(symbol, from, to);
            } catch (RuntimeException error) {
                last = error;
                if (attempt == attempts) {
                    break;
                }
                sleepBackoff(attempt);
            }
        }
        throw new IllegalStateException("Failed to retrieve Schwab market data for " + symbol + " after retries", last);
    }

    private void sleepBackoff(int attempt) {
        long sleepMs = Math.max(0L, properties.getRetryBackoffMillis()) * attempt;
        if (sleepMs == 0L) return;
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private List<MarketBar> fetchBars(String symbol, LocalDate from, LocalDate to) {
        try {
            String json = restClient.get()
                    .uri(buildUri(symbol, from, to))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSupplier.currentAccessToken())
                    .retrieve()
                    .body(String.class);
            SchwabPriceHistoryResponse response = objectMapper.readValue(json, SchwabPriceHistoryResponse.class);
            return toMarketBars(symbol, response);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to retrieve Schwab market data for " + symbol, error);
        }
    }

    private URI buildUri(String symbol, LocalDate from, LocalDate to) {
        long startMillis = from.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endMillis = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1;
        return URI.create(properties.getBaseUrl()
                + "/pricehistory?symbol=" + symbol.trim().toUpperCase()
                + "&periodType=year&frequencyType=daily&frequency=1"
                + "&startDate=" + startMillis
                + "&endDate=" + endMillis);
    }

    private List<MarketBar> toMarketBars(String symbol, SchwabPriceHistoryResponse response) {
        if (response == null || response.candles() == null) return List.of();
        return response.candles().stream()
                .map(candle -> new MarketBar(symbol, Instant.ofEpochMilli(candle.datetime()).atZone(ZoneOffset.UTC).toLocalDate(), candle.open(), candle.high(), candle.low(), candle.close(), candle.volume()))
                .sorted(Comparator.comparing(MarketBar::date))
                .toList();
    }
}
