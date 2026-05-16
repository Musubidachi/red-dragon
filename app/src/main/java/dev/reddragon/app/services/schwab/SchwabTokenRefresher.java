package dev.reddragon.app.services.schwab;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled background job: every 60s, call
 * {@link SchwabOAuthService#refreshIfNearExpiry()} so the access token never
 * runs out while the trader is mid-session.
 *
 * <p>The job is a no-op when the latest stored token is still fresh, and a
 * no-op when no token has been issued yet (waiting for the OAuth flow).
 *
 * <p>Only active when {@code red-dragon.schwab.client-id} is configured —
 * the bean isn't created in fallback (static-token) mode.
 */
@Component
@ConditionalOnBean(SchwabOAuthService.class)
@RequiredArgsConstructor
@Slf4j
public class SchwabTokenRefresher {

    private final SchwabOAuthService oauthService;

    @Scheduled(fixedDelayString = "${red-dragon.schwab.token-refresh-interval-millis:60000}")
    public void run() {
        try {
            oauthService.refreshIfNearExpiry();
        } catch (RuntimeException error) {
            log.warn("Schwab token refresher run failed: {}", error.getMessage());
        }
    }
}
