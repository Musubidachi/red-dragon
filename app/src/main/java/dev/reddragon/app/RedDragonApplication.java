package dev.reddragon.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Single bootable entry point for the candidate-in / verdict-out platform.
 */
@SpringBootApplication(scanBasePackages = "dev.reddragon")
@ConfigurationPropertiesScan(basePackages = "dev.reddragon.app.config")
@EntityScan("dev.reddragon.persistence.domains")
@EnableJpaRepositories("dev.reddragon.persistence.services.repositories")
public class RedDragonApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedDragonApplication.class, args);
    }
}
