package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trivial liveness endpoint, separate from Spring Boot Actuator's /actuator/health
 * so callers don't need to know about Actuator.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "red-dragon",
                "timestamp", Instant.now().toString());
    }
}
