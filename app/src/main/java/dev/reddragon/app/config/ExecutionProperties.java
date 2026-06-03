package dev.reddragon.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import dev.reddragon.execution.ExecutionMode;

@ConfigurationProperties(prefix = "red-dragon.execution")
public record ExecutionProperties(
        ExecutionMode mode,
        boolean liveEnabled
) {
    public ExecutionProperties {
        if (mode == null) {
            mode = ExecutionMode.DRY_RUN;
        }
    }
}
