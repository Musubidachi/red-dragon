package dev.reddragon.app.services.schwab;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import dev.reddragon.app.models.SchwabTokenResponse;
import dev.reddragon.marketdata.config.SchwabOAuthProperties;
import dev.reddragon.marketdata.services.provider.schwab.SchwabAccessTokenSupplier;
import dev.reddragon.persistence.domains.SchwabTokenEntity;
import dev.reddragon.persistence.services.repositories.SchwabTokenRepository;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;

/**
 * Owns the Schwab OAuth lifecycle:
 * <ol>
 *   <li>Build the authorization URL the trader visits to grant access.</li>
 *   <li>Exchange the resulting authorization code for an access + refresh
 *       token pair, then persist it to the database.</li>
 *   <li>Refresh the access token using the stored refresh token, on demand
 *       or via a scheduled job.</li>
 *   <li>Expose {@link #currentAccessToken()} so the market-data provider
 *       always sees a fresh token without knowing anything about OAuth.</li>
 * </ol>
 *
 * <p>This class is the bridge between {@code lib-marketdata}'s
 * {@link SchwabAccessTokenSupplier} interface and {@code lib-persistence}'s
 * {@code SchwabTokenRepository}. It lives in {@code app} because that is
 * the only place those two layers meet.
 *
 * <p>Tokens are considered "near expiry" when their remaining lifetime is
 * below {@link #REFRESH_WINDOW}. The {@link #refreshIfNearExpiry()} method
 * (called by a scheduled job in {@code app/pipeline}) does a one-off
 * proactive refresh when that threshold is crossed.
 */
@Slf4j
public class SchwabOAuthService implements SchwabAccessTokenSupplier {

    /** Refresh the access token when fewer than this many seconds remain. */
    private static final long REFRESH_WINDOW_SECONDS = 120;

    private final SchwabOAuthProperties oauthProperties;
    private final SchwabTokenRepository tokenRepository;
    private final RestClient restClient;

    public SchwabOAuthService(
            SchwabOAuthProperties oauthProperties,
            SchwabTokenRepository tokenRepository,
            RestClient restClient
    ) {
        this.oauthProperties = Objects.requireNonNull(oauthProperties, "oauthProperties is required");
        this.tokenRepository = Objects.requireNonNull(tokenRepository, "tokenRepository is required");
        this.restClient = Objects.requireNonNull(restClient, "restClient is required");
    }

    /**
     * URL the trader visits in a browser to grant the app access. Schwab will
     * redirect back to {@code oauthProperties.redirectUri} with an
     * authorization {@code code} query parameter that the caller then posts
     * to {@link #exchangeAuthorizationCode(String)}.
     */
    public String authorizeUrl() {
        // Schwab uses the standard authorization-code flow; the consent URL is
        // exposed at <baseAuthUrl>/authorize. We derive it from the token URL
        // by replacing the trailing /token with /authorize.
        String baseAuth = oauthProperties.getTokenUrl().replaceFirst("/token$", "/authorize");
        return baseAuth
                + "?response_type=code"
                + "&client_id=" + encode(oauthProperties.getClientId())
                + "&redirect_uri=" + encode(oauthProperties.getRedirectUri())
                + "&scope=" + encode("openid profile");
    }

    /**
     * Exchange the authorization {@code code} for an access + refresh token
     * pair and persist the result. Called by the OAuth callback controller.
     */
    public SchwabTokenEntity exchangeAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("authorization code is required");
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",   "authorization_code");
        body.add("code",         code);
        body.add("redirect_uri", oauthProperties.getRedirectUri());
        SchwabTokenResponse response = postToTokenEndpoint(body);
        return persistFromResponse(response);
    }

    /**
     * Refresh the access token using the most recent persisted refresh token.
     * No-op if no token has ever been persisted (caller must call
     * {@link #exchangeAuthorizationCode(String)} first).
     */
    @Timed(value = "reddragon.schwab.token.refresh", description = "Schwab token refresh duration")
    public SchwabTokenEntity refreshAccessToken() {
        SchwabTokenEntity latest = tokenRepository.findTopByOrderByIssuedAtDesc();
        String refreshToken = latest == null
                ? oauthProperties.getRefreshToken()
                : latest.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("No refresh token available; complete the OAuth authorize flow first.");
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "refresh_token");
        body.add("refresh_token", refreshToken);
        SchwabTokenResponse response = postToTokenEndpoint(body);
        return persistFromResponse(response);
    }

    /**
     * Called by the scheduled refresher in {@code app/pipeline}. Performs a
     * refresh only if the most recent token is missing or within the refresh
     * window of expiry.
     */
    public void refreshIfNearExpiry() {
        SchwabTokenEntity latest = tokenRepository.findTopByOrderByIssuedAtDesc();
        if (latest == null) {
            log.debug("Schwab token refresh skipped: no token persisted yet.");
            return;
        }
        Instant threshold = Instant.now().plusSeconds(REFRESH_WINDOW_SECONDS);
        if (latest.getExpiresAt().isAfter(threshold)) {
            return; // still fresh enough
        }
        log.info("Refreshing Schwab access token; current expires at {}", latest.getExpiresAt());
        try {
            refreshAccessToken();
        } catch (RuntimeException error) {
            log.warn("Schwab token refresh failed: {}", error.getMessage());
        }
    }

    @Override
    public String currentAccessToken() {
        SchwabTokenEntity latest = tokenRepository.findTopByOrderByIssuedAtDesc();
        if (latest != null && latest.getExpiresAt().isAfter(Instant.now())) {
            return latest.getAccessToken();
        }
        // Either no token has been issued, or the latest one expired —
        // attempt a one-off refresh inline. If that also fails, surface
        // the underlying error to the caller.
        return refreshAccessToken().getAccessToken();
    }

    // ---- internals ---------------------------------------------------------

    private SchwabTokenResponse postToTokenEndpoint(MultiValueMap<String, String> form) {
        SchwabTokenResponse response = restClient.post()
                .uri(oauthProperties.getTokenUrl())
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(SchwabTokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Schwab token endpoint returned no access_token");
        }
        return response;
    }

    private SchwabTokenEntity persistFromResponse(SchwabTokenResponse response) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(response.expiresInSeconds(), ChronoUnit.SECONDS);
        String tokenType = response.tokenType() == null ? "Bearer" : response.tokenType();
        SchwabTokenEntity entity = new SchwabTokenEntity(
                null,
                response.accessToken(),
                response.refreshToken(),
                issuedAt,
                expiresAt,
                tokenType
        );
        return tokenRepository.save(entity);
    }

    private String basicAuthHeader() {
        String credentials = oauthProperties.getClientId() + ":" + oauthProperties.getClientSecret();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
