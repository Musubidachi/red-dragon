package dev.reddragon.app.controllers;

import dev.reddragon.validation.config.ValidationProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Describes the available validation profiles.
 *
 * <pre>
 * GET /api/validation/profiles
 * </pre>
 */
@RestController
@RequestMapping("/api/validation/profiles")
public class ValidationProfilesController {

    @GetMapping
    public List<Map<String, String>> profiles() {
        return Arrays.stream(ValidationProfile.values())
                .map(p -> Map.of(
                        "name",        p.name(),
                        "displayName", p.displayName()
                ))
                .toList();
    }
}
