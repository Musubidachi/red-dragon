package dev.reddragon.marketdata.provider.schwab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reddragon.marketdata.model.MarketBar;
import dev.reddragon.marketdata.provider.MarketDataProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schwab daily-price history adapter.
 *
 * <p>The adapter deliberately exposes only normalized {@link MarketBar}s to the
 * rest of the platform. Authentication, response shape, caching, and provider
 * errors stay contained here.
 */
public class SchwabMarketDataProvider implements MarketDataProvider {

    private final SchwabMarketDataProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public SchwabMarketDataProvider(SchwabMarketDataProperties properties) {
        this(properties, RestClient.create(), new ObjectMapper());
    }

    public SchwabMarketDataProvider(
            SchwabMarketDataProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.properties = Objects.requireNonNull(properties, "properties is required");
        this.restClient = Objects.requireNonNull(restClient, "restClient is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
    }

    @Override
    public List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to) {
        validate(symbol, from, to);
        if (!properties.configured()) {
            return List.of();
        }

        String cacheKey = cacheKey(symbol, from, to);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.expired(properties.getCacheTtlSeconds())) {
            return cached.bars();
        }

        List<MarketBar> bars = fetchBars(symbol, from, to);
        cache.put(cacheKey, new CacheEntry(Instant.now(), bars));
        return bars;
    }

    private void validate(String symbol, LocalDate from, LocalDate to) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to dates are required");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to date cannot be before from date");
        }
    }

    private String cacheKey(String symbol, LocalDate from, LocalDate to) {
        return symbol.trim().toUpperCase() + ":" + from + ":" + to;
    }

    private List<MarketBar> fetchBars(String symbol, LocalDate from, LocalDate to) {
        try {
            String json = restClient.get()
                    .uri(buildUri(symbol, from, to))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
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
        if (response == null || response.candles() == null) {
            return List.of();
        }

        return response.candles().stream()
                .map(candle -> new MarketBar(
                        symbol,
                        Instant.ofEpochMilli(candle.datetime()).atZone(ZoneOffset.UTC).toLocalDate(),
                        candle.open(),
                        candle.high(),
                        candle.low(),
                        candle.close(),
                        candle.volume()
                ))
                .sorted(Comparator.comparing(MarketBar::date))
                .toList();
    }

    private record CacheEntry(Instant storedAt, List<MarketBar> bars) {
        boolean expired(long ttlSeconds) {
            return ttlSeconds <= 0 || Instant.now().isAfter(storedAt.plusSeconds(ttlSeconds));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SchwabPriceHistoryResponse(List<SchwabCandle> candles) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SchwabCandle(
            double open,
            double high,
            double low,
            double close,
            long volume,
            long datetime
    ) {
    }
}
