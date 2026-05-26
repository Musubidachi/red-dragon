package dev.reddragon.marketdata.services.provider.schwab;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.marketdata.config.SchwabMarketDataProperties;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;
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

    @Override
    public List<IntradayBar> intradayBars(String symbol, Instant from, Instant to, Duration interval) {
        validateIntraday(symbol, from, to, interval);
        if (!properties.configured()) {
            return List.of();
        }
        List<SchwabCandle> candles = fetchPriceHistory(buildIntradayUri(symbol, from, to, interval));
        return candles.stream()
                .map(candle -> new IntradayBar(
                        symbol,
                        Instant.ofEpochMilli(candle.datetime()),
                        candle.open(),
                        candle.high(),
                        candle.low(),
                        candle.close(),
                        candle.volume(),
                        typicalPrice(candle)
                ))
                .sorted(Comparator.comparing(IntradayBar::startTime))
                .toList();
    }

    @Override
    public MarketQuote quote(String symbol) {
        validateSymbol(symbol);
        if (!properties.configured()) {
            return MarketQuote.unavailable(symbol, "Schwab market data is not configured.");
        }
        try {
            String normalized = symbol.trim().toUpperCase();
            String json = restClient.get()
                    .uri(properties.getBaseUrl() + "/quotes?symbols=" + normalized)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSupplier.currentAccessToken())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode node = root.path(normalized);
            JsonNode quote = node.path("quote");
            double last = firstPositive(
                    quote.path("lastPrice").asDouble(0.0),
                    quote.path("mark").asDouble(0.0),
                    quote.path("closePrice").asDouble(0.0)
            );
            // No usable price → return unavailable so the composite chain
            // falls back to the next provider instead of caching a synthetic
            // EMPTY_BARS quote that masks the gap.
            if (last <= 0.0) {
                return MarketQuote.unavailable(normalized,
                        "Schwab quote did not include a usable last price.");
            }
            return new MarketQuote(
                    normalized,
                    quoteInstant(quote),
                    last,
                    quote.path("bidPrice").asDouble(0.0),
                    quote.path("askPrice").asDouble(0.0),
                    quote.path("totalVolume").asLong(0L),
                    MarketDataQuality.COMPLETE,
                    List.of("Provider: Schwab")
            );
        } catch (Exception error) {
            throw new IllegalStateException("Failed to retrieve Schwab quote for " + symbol, error);
        }
    }

    @Override
    public String providerName() {
        return "schwab";
    }

    private void validate(String symbol, LocalDate from, LocalDate to) {
        validateSymbol(symbol);
        if (from == null || to == null) throw new IllegalArgumentException("from and to dates are required");
        if (to.isBefore(from)) throw new IllegalArgumentException("to date cannot be before from date");
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
    }

    private void validateIntraday(String symbol, Instant from, Instant to, Duration interval) {
        validateSymbol(symbol);
        if (from == null || to == null) throw new IllegalArgumentException("from and to instants are required");
        if (to.isBefore(from)) throw new IllegalArgumentException("to instant cannot be before from instant");
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive");
        }
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
            return toMarketBars(symbol, fetchPriceHistory(buildUri(symbol, from, to)));
        } catch (IllegalStateException error) {
            // fetchPriceHistory already wraps in IllegalStateException with a clear
            // root cause; rethrow as-is rather than nesting another layer.
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Failed to retrieve Schwab market data for " + symbol, error);
        }
    }

    private List<SchwabCandle> fetchPriceHistory(URI uri) {
        try {
            String json = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSupplier.currentAccessToken())
                    .retrieve()
                    .body(String.class);
            SchwabPriceHistoryResponse response = objectMapper.readValue(json, SchwabPriceHistoryResponse.class);
            return response == null || response.candles() == null ? List.of() : response.candles();
        } catch (Exception error) {
            throw new IllegalStateException("Failed to retrieve Schwab price history", error);
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

    private URI buildIntradayUri(String symbol, Instant from, Instant to, Duration interval) {
        long minutes = Math.max(1L, interval.toMinutes());
        return URI.create(properties.getBaseUrl()
                + "/pricehistory?symbol=" + symbol.trim().toUpperCase()
                + "&periodType=day&frequencyType=minute&frequency=" + minutes
                + "&startDate=" + from.toEpochMilli()
                + "&endDate=" + to.toEpochMilli()
                + "&needExtendedHoursData=true");
    }

    private List<MarketBar> toMarketBars(String symbol, List<SchwabCandle> candles) {
        if (candles == null) return List.of();
        return candles.stream()
                .map(candle -> new MarketBar(symbol, Instant.ofEpochMilli(candle.datetime()).atZone(ZoneOffset.UTC).toLocalDate(), candle.open(), candle.high(), candle.low(), candle.close(), candle.volume()))
                .sorted(Comparator.comparing(MarketBar::date))
                .toList();
    }

    private double typicalPrice(SchwabCandle candle) {
        return (candle.high() + candle.low() + candle.close()) / 3.0;
    }

    private Instant quoteInstant(JsonNode quote) {
        long quoteTime = quote.path("quoteTimeInLong").asLong(0L);
        if (quoteTime <= 0L) {
            return Instant.now();
        }
        return Instant.ofEpochMilli(quoteTime);
    }

    private double firstPositive(double... values) {
        for (double value : values) {
            if (value > 0.0) {
                return value;
            }
        }
        return 0.0;
    }
}
