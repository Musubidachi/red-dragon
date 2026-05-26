package dev.reddragon.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import dev.reddragon.app.config.ValidationThresholdsProperties;

/**
 * Single bootable entry point for the candidate-in / verdict-out platform.
 */
@SpringBootApplication(scanBasePackages = "dev.reddragon")
@EnableConfigurationProperties(ValidationThresholdsProperties.class)
@EntityScan("dev.reddragon.persistence.domains")
@EnableJpaRepositories("dev.reddragon.persistence.services.repositories")
public class RedDragonApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedDragonApplication.class, args);
    }
}
