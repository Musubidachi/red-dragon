package dev.reddragon.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "red-dragon.llm.research")
public record LlmResearchProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        boolean webSearchEnabled,
        String webSearchToolType
) {
    public LlmResearchProperties {
        if (apiKey == null) {
            apiKey = "";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4.1-mini";
        }
        if (webSearchToolType == null || webSearchToolType.isBlank()) {
            webSearchToolType = "web_search";
        }
    }

    public boolean configured() {
        return enabled && !apiKey.isBlank() && !baseUrl.isBlank();
    }
}
