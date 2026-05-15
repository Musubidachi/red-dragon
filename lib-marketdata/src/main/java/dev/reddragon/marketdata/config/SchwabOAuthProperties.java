package dev.reddragon.marketdata.config;

import lombok.Value;

/**
 * OAuth 2.0 configuration for Schwab — separate from
 * {@link SchwabMarketDataProperties} so the market-data adapter can remain
 * usable with a static bearer token in non-prod environments.
 *
 * <p>When {@link #configured()} returns true, the token-refresh service
 * (see {@code SchwabTokenService}) takes over from the static
 * {@code SchwabMarketDataProperties.accessToken}. The static token is the
 * fallback path used when these OAuth fields are empty.
 *
 * <p>The redirect URI must match what is registered in the Schwab developer
 * portal — typically a {@code https://127.0.0.1:8080/api/schwab/oauth/callback}
 * for local development.
 */
@Value
public class SchwabOAuthProperties {

    /** Client id from the Schwab developer portal. */
    String clientId;

    /** Client secret from the Schwab developer portal. */
    String clientSecret;

    /**
     * Persisted refresh token, if one has already been obtained.
     * When this is non-blank at startup, the token service will seed itself
     * with this refresh token instead of waiting for an OAuth callback.
     */
    String refreshToken;

    /** OAuth callback URL — must match the registered redirect URI. */
    String redirectUri;

    /** Schwab token endpoint URL. */
    String tokenUrl;

    /**
     * @return {@code true} if clientId, clientSecret, and tokenUrl are all
     *         configured. Refresh token may still be empty (will be obtained
     *         via the authorize → callback flow).
     */
    public boolean configured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && tokenUrl != null && !tokenUrl.isBlank();
    }
}
