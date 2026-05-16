package dev.reddragon.marketdata.services.provider.yahoo;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.marketdata.config.YahooMarketDataProperties;
import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketQuote;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;

/**
 * Secondary market-data adapter using Yahoo's chart endpoint.
 */
public class YahooMarketDataProvider implements MarketDataProvider {

    private final YahooMarketDataProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YahooMarketDataProvider(YahooMarketDataProperties properties) {
        this(properties, RestClient.create(), new ObjectMapper());
    }

    public YahooMarketDataProvider(
            YahooMarketDataProperties properties,
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
        JsonNode result = fetchChart(symbol, from.atStartOfDay().toInstant(ZoneOffset.UTC), to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC), "1d");
        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = firstQuote(result);
        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");

        return java.util.stream.IntStream.range(0, timestamps.size())
                .filter(index -> usable(opening(opens, index), opening(highs, index), opening(lows, index), opening(closes, index)))
                .mapToObj(index -> new MarketBar(
                        symbol,
                        Instant.ofEpochSecond(timestamps.get(index).asLong()).atZone(ZoneOffset.UTC).toLocalDate(),
                        opens.get(index).asDouble(),
                        highs.get(index).asDouble(),
                        lows.get(index).asDouble(),
                        closes.get(index).asDouble(),
                        volumes.path(index).asLong(0L)
                ))
                .sorted(Comparator.comparing(MarketBar::date))
                .toList();
    }

    @Override
    public List<IntradayBar> intradayBars(String symbol, Instant from, Instant to, Duration interval) {
        validateIntraday(symbol, from, to, interval);
        if (!properties.configured()) {
            return List.of();
        }
        String yahooInterval = yahooInterval(interval);
        JsonNode result = fetchChart(symbol, from, to, yahooInterval);
        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = firstQuote(result);
        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");

        return java.util.stream.IntStream.range(0, timestamps.size())
                .filter(index -> usable(opening(opens, index), opening(highs, index), opening(lows, index), opening(closes, index)))
                .mapToObj(index -> new IntradayBar(
                        symbol,
                        Instant.ofEpochSecond(timestamps.get(index).asLong()),
                        opens.get(index).asDouble(),
                        highs.get(index).asDouble(),
                        lows.get(index).asDouble(),
                        closes.get(index).asDouble(),
                        volumes.path(index).asLong(0L),
                        typicalPrice(highs.get(index).asDouble(), lows.get(index).asDouble(), closes.get(index).asDouble())
                ))
                .sorted(Comparator.comparing(IntradayBar::startTime))
                .toList();
    }

    @Override
    public MarketQuote quote(String symbol) {
        validateSymbol(symbol);
        if (!properties.configured()) {
            return MarketQuote.unavailable(symbol, "Yahoo market data is not configured.");
        }
        JsonNode result = fetchChart(symbol, Instant.now().minus(Duration.ofDays(5)), Instant.now(), "1d");
        JsonNode meta = result.path("meta");
        double regularMarketPrice = meta.path("regularMarketPrice").asDouble(0.0);
        JsonNode quote = firstQuote(result);
        JsonNode timestamps = result.path("timestamp");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");
        int lastIndex = lastUsableIndex(closes);
        double last = regularMarketPrice > 0.0 ? regularMarketPrice : (lastIndex >= 0 ? closes.get(lastIndex).asDouble() : 0.0);
        long volume = lastIndex >= 0 ? volumes.path(lastIndex).asLong(0L) : 0L;
        Instant observedAt = lastIndex >= 0 && timestamps.has(lastIndex)
                ? Instant.ofEpochSecond(timestamps.get(lastIndex).asLong())
                : Instant.now();
        return new MarketQuote(
                symbol,
                observedAt,
                last,
                0.0,
                0.0,
                volume,
                last > 0.0 ? MarketDataQuality.COMPLETE : MarketDataQuality.EMPTY_BARS,
                last > 0.0 ? List.of("Provider: Yahoo chart") : List.of("Yahoo chart did not include a usable price.")
        );
    }

    @Override
    public String providerName() {
        return "yahoo";
    }

    private JsonNode fetchChart(String symbol, Instant from, Instant to, String interval) {
        RuntimeException last = null;
        int attempts = Math.max(1, properties.getMaxRetries() + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return fetchChartOnce(symbol, from, to, interval);
            } catch (RuntimeException error) {
                last = error;
                if (attempt == attempts) {
                    break;
                }
                sleepBackoff(attempt);
            }
        }
        throw new IllegalStateException("Failed to retrieve Yahoo market data for " + symbol + " after retries", last);
    }

    private JsonNode fetchChartOnce(String symbol, Instant from, Instant to, String interval) {
        try {
            String json = restClient.get()
                    .uri(buildUri(symbol, from, to, interval))
                    .retrieve()
                    .body(String.class);
            JsonNode result = objectMapper.readTree(json)
                    .path("chart")
                    .path("result")
                    .path(0);
            if (result.isMissingNode() || result.isNull()) {
                return objectMapper.createObjectNode();
            }
            return result;
        } catch (Exception error) {
            throw new IllegalStateException("Failed to retrieve Yahoo market data for " + symbol, error);
        }
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

    private URI buildUri(String symbol, Instant from, Instant to, String interval) {
        return URI.create(properties.getBaseUrl()
                + "/v8/finance/chart/" + symbol.trim().toUpperCase()
                + "?period1=" + from.getEpochSecond()
                + "&period2=" + to.getEpochSecond()
                + "&interval=" + interval);
    }

    private JsonNode firstQuote(JsonNode result) {
        return result.path("indicators").path("quote").path(0);
    }

    private void validate(String symbol, LocalDate from, LocalDate to) {
        validateSymbol(symbol);
        if (from == null || to == null) throw new IllegalArgumentException("from and to dates are required");
        if (to.isBefore(from)) throw new IllegalArgumentException("to date cannot be before from date");
    }

    private void validateIntraday(String symbol, Instant from, Instant to, Duration interval) {
        validateSymbol(symbol);
        if (from == null || to == null) throw new IllegalArgumentException("from and to instants are required");
        if (to.isBefore(from)) throw new IllegalArgumentException("to instant cannot be before from instant");
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive");
        }
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
    }

    private String yahooInterval(Duration interval) {
        long minutes = Math.max(1L, interval.toMinutes());
        if (minutes <= 1L) return "1m";
        if (minutes <= 2L) return "2m";
        if (minutes <= 5L) return "5m";
        if (minutes <= 15L) return "15m";
        if (minutes <= 30L) return "30m";
        if (minutes <= 60L) return "60m";
        return "1d";
    }

    private double opening(JsonNode values, int index) {
        JsonNode value = values.path(index);
        return value.isMissingNode() || value.isNull() ? 0.0 : value.asDouble(0.0);
    }

    private boolean usable(double open, double high, double low, double close) {
        return open > 0.0 && high > 0.0 && low > 0.0 && close > 0.0;
    }

    private int lastUsableIndex(JsonNode closes) {
        for (int index = closes.size() - 1; index >= 0; index--) {
            JsonNode close = closes.path(index);
            if (!close.isMissingNode() && !close.isNull() && close.asDouble(0.0) > 0.0) {
                return index;
            }
        }
        return -1;
    }

    private double typicalPrice(double high, double low, double close) {
        return (high + low + close) / 3.0;
    }
}
