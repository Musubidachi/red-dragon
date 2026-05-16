package dev.reddragon.marketdata.services.provider.schwab;

/**
 * Single-method strategy for "what is the current valid Schwab access token?".
 *
 * <p>The market-data provider asks this on every call so it can transparently
 * pick up tokens that were refreshed in the background. Two implementations
 * exist:
 *
 * <ul>
 *   <li>{@link StaticSchwabAccessTokenSupplier} — returns the static token
 *       baked into {@link SchwabMarketDataProperties}. Used when OAuth is
 *       not configured.</li>
 *   <li>(in {@code app}) {@code OAuthSchwabAccessTokenSupplier} — talks to
 *       Schwab's {@code /oauth/token} endpoint and persists the refreshed
 *       token to the database. Used when OAuth is configured.</li>
 * </ul>
 */
@FunctionalInterface
public interface SchwabAccessTokenSupplier {

    /**
     * @return the current valid bearer token to put into the
     *         {@code Authorization} header on outbound calls.
     */
    String currentAccessToken();
}
