package dev.reddragon.marketdata.config;

import lombok.Value;

/**
 * Runtime configuration for Schwab market-data access.
 */
@Value
public class SchwabMarketDataProperties {
    String baseUrl;
    String accessToken;
    boolean enabled;
    long cacheTtlSeconds;
    int maxRetries;
    long retryBackoffMillis;

    public boolean configured() {
        return enabled && accessToken != null && !accessToken.isBlank();
    }
}
