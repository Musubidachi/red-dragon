package dev.reddragon.marketdata.config;

import lombok.Value;

/**
 * Runtime configuration for the Yahoo chart fallback provider.
 */
@Value
public class YahooMarketDataProperties {
    String baseUrl;
    boolean enabled;
    long cacheTtlSeconds;
    int maxRetries;
    long retryBackoffMillis;

    public boolean configured() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }
}
