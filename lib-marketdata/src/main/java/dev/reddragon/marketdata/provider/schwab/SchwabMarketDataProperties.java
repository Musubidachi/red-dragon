package dev.reddragon.marketdata.provider.schwab;

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
