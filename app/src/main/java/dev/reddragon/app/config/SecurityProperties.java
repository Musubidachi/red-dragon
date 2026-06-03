package dev.reddragon.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "red-dragon.security")
public record SecurityProperties(String apiKey) {
    public SecurityProperties {
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "dev-red-dragon-api-key";
        }
    }
}
