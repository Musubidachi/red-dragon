package dev.reddragon.app.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "red-dragon.cors")
public record WebCorsProperties(List<String> allowedOriginPatterns) {
    public WebCorsProperties {
        if (allowedOriginPatterns == null) {
            allowedOriginPatterns = List.of();
        } else {
            allowedOriginPatterns = allowedOriginPatterns.stream()
                    .filter(pattern -> pattern != null && !pattern.isBlank())
                    .toList();
        }
    }
}
