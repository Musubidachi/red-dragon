package dev.reddragon.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("dev.reddragon.persistence.domains")
@EnableJpaRepositories("dev.reddragon.persistence.services.repositories")
public class PersistenceIntegrationTestApplication {
}
