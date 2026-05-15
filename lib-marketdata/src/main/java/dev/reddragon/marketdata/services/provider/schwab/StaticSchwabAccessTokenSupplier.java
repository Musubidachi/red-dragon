package dev.reddragon.marketdata.services.provider.schwab;

import dev.reddragon.marketdata.config.SchwabMarketDataProperties;
import lombok.RequiredArgsConstructor;

/**
 * Default {@link SchwabAccessTokenSupplier} that simply returns the static
 * token field from {@link SchwabMarketDataProperties}.
 *
 * <p>Used when OAuth is not configured. Switch to the OAuth-backed supplier
 * (in {@code app}) once a refresh token is available.
 */
@RequiredArgsConstructor
public class StaticSchwabAccessTokenSupplier implements SchwabAccessTokenSupplier {

    private final SchwabMarketDataProperties properties;

    @Override
    public String currentAccessToken() {
        return properties.getAccessToken();
    }
}
