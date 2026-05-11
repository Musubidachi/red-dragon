package dev.reddragon.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Single bootable entry point.
 *
 * <p>The starter app runs without a configured database so the candidate
 * pipeline can be exercised before persistence runtime wiring is added.
 */
@SpringBootApplication(
        scanBasePackages = "dev.reddragon",
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                FlywayAutoConfiguration.class
        }
)
public class RedDragonApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedDragonApplication.class, args);
    }
}
