package dev.reddragon.app.config;

import lombok.Value;

@Value
public class LlmResearchProperties {
    boolean enabled;
    String apiKey;
    String baseUrl;
    String model;
    boolean webSearchEnabled;
    String webSearchToolType;

    public boolean configured() {
        return enabled && apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }
}
