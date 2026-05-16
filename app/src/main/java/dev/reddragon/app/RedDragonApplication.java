package dev.reddragon.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import dev.reddragon.app.config.ValidationThresholdsProperties;

/**
 * Single bootable entry point for the candidate-in / verdict-out platform.
 */
@SpringBootApplication(scanBasePackages = "dev.reddragon")
@EnableConfigurationProperties(ValidationThresholdsProperties.class)
public class RedDragonApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedDragonApplication.class, args);
    }
}
