package dev.reddragon.app.controllers;

import dev.reddragon.app.services.schwab.SchwabOAuthService;
import dev.reddragon.persistence.domains.SchwabTokenEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * HTTP entry points for the Schwab OAuth 2.0 three-legged flow.
 *
 * <pre>
 * 1. GET  /api/schwab/oauth/authorize-url   -> tells the trader where to grant consent
 * 2. (browser) Schwab -> redirect_uri        -> /api/schwab/oauth/callback?code=...
 * 3. GET  /api/schwab/oauth/callback        -> exchanges the code, persists the token
 * 4. POST /api/schwab/oauth/refresh         -> manual refresh (otherwise refreshed on demand)
 * </pre>
 *
 * <p>The controller is only useful when OAuth is configured
 * ({@code red-dragon.schwab.client-id} et al). When the OAuth service bean is
 * absent, this controller is not registered and the system falls back to the
 * static access-token supplier used by the market-data provider.
 */
@RestController
@RequestMapping("/api/schwab/oauth")
@ConditionalOnBean(SchwabOAuthService.class)
@RequiredArgsConstructor
@Slf4j
public class SchwabOAuthController {

    private final SchwabOAuthService oauthService;

    @GetMapping("/authorize-url")
    public Map<String, String> authorizeUrl() {
        return Map.of("authorizeUrl", oauthService.authorizeUrl());
    }

    @GetMapping("/callback")
    public Map<String, Object> callback(@RequestParam("code") String code) {
        SchwabTokenEntity token = oauthService.exchangeAuthorizationCode(code);
        log.info("Schwab OAuth callback completed; new token expires at {}", token.getExpiresAt());
        return Map.of(
                "tokenType", token.getTokenType(),
                "issuedAt",  token.getIssuedAt().toString(),
                "expiresAt", token.getExpiresAt().toString()
        );
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        SchwabTokenEntity token = oauthService.refreshAccessToken();
        return Map.of(
                "tokenType", token.getTokenType(),
                "issuedAt",  token.getIssuedAt().toString(),
                "expiresAt", token.getExpiresAt().toString()
        );
    }
}
