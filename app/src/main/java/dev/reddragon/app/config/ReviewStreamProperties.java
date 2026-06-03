package dev.reddragon.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "red-dragon.review")
public record ReviewStreamProperties(
        long streamTimeoutMs,
        long streamRefreshMs
) {
    public ReviewStreamProperties {
        if (streamTimeoutMs <= 0L) {
            streamTimeoutMs = 1_800_000L;
        }
        if (streamRefreshMs <= 0L) {
            streamRefreshMs = 15_000L;
        }
    }
}
