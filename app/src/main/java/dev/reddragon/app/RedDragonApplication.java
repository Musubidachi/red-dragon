package dev.reddragon.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single bootable entry point for the candidate-in / verdict-out platform.
 */
@SpringBootApplication(scanBasePackages = "dev.reddragon")
public class RedDragonApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedDragonApplication.class, args);
    }
}
