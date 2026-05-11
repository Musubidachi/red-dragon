package dev.reddragon.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single bootable entry point.
 *
 * <p>Component scanning is restricted to the {@code dev.reddragon} root so the
 * library modules can contribute beans without each defining their own
 * configuration class. Keep this class small; wiring belongs in {@code @Configuration}
 * classes within each lib.
 */
@SpringBootApplication(scanBasePackages = "dev.reddragon")
public class RedDragonApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedDragonApplication.class, args);
    }
}
