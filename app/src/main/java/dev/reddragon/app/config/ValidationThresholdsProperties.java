package dev.reddragon.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.reddragon.validation.config.ValidationThresholdProperties;
import dev.reddragon.validation.config.ValidationThresholds;

/**
 * App wiring wrapper for {@code red-dragon.validation.*}.
 *
 * <p>The bindable threshold fields live in {@link ValidationThresholdProperties}
 * so lib-validation owns its own threshold surface. This class only exposes
 * the bound values as the app's default {@link ValidationThresholds} bean.
 */
@Configuration
@ConfigurationProperties(prefix = ValidationThresholdProperties.PREFIX)
public class ValidationThresholdsProperties extends ValidationThresholdProperties {

    @Bean
    public ValidationThresholds defaultThresholds() {
        return toThresholds();
    }
}
