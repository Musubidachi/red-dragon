package dev.reddragon.marketdata.services.provider;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;

/**
 * Ordered fallback chain for market data providers.
 *
 * <p>Each provider in the chain is tried in order. A provider that throws is
 * logged at WARN level and the chain advances to the next provider. A provider
 * that returns an empty result is treated as "no data" and the chain also
 * advances (without a WARN, since empty is a legitimate outcome).
 */
@Slf4j
public class CompositeMarketDataProvider implements MarketDataProvider {

    private final List<MarketDataProvider> providers;

    public CompositeMarketDataProvider(List<MarketDataProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers is required"));
    }

    @Override
    public List<MarketBar> historicalDailyBars(String symbol, LocalDate from, LocalDate to) {
        for (MarketDataProvider provider : providers) {
            try {
                List<MarketBar> bars = provider.historicalDailyBars(symbol, from, to);
                if (bars != null && !bars.isEmpty()) {
                    return bars;
                }
            } catch (RuntimeException error) {
                log.warn("Provider {} failed historicalDailyBars for {}; trying next provider",
                        provider.providerName(), symbol, error);
            }
        }
        return List.of();
    }

    @Override
    public List<IntradayBar> intradayBars(String symbol, Instant from, Instant to, Duration interval) {
        for (MarketDataProvider provider : providers) {
            try {
                List<IntradayBar> bars = provider.intradayBars(symbol, from, to, interval);
                if (bars != null && !bars.isEmpty()) {
                    return bars;
                }
            } catch (RuntimeException error) {
                log.warn("Provider {} failed intradayBars for {}; trying next provider",
                        provider.providerName(), symbol, error);
            }
        }
        return List.of();
    }

    @Override
    public MarketQuote quote(String symbol) {
        for (MarketDataProvider provider : providers) {
            try {
                MarketQuote quote = provider.quote(symbol);
                if (quote != null && quote.available()) {
                    return quote;
                }
            } catch (RuntimeException error) {
                log.warn("Provider {} failed quote for {}; trying next provider",
                        provider.providerName(), symbol, error);
            }
        }
        return MarketQuote.unavailable(symbol, "No configured provider returned a live quote.");
    }

    @Override
    public String providerName() {
        return "CompositeMarketDataProvider";
    }
}
