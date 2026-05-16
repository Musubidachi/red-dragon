package dev.reddragon.app.services.pipeline;

import dev.reddragon.validation.config.ValidationProfile;
import dev.reddragon.validation.config.ValidationThresholdProfileFactory;
import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.services.ValidationService;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Caches one {@link ValidationService} per {@link ValidationProfile} so the
 * pipeline can switch profiles per-request without allocating new services.
 */
@Component
public class ValidationServiceFactory {

    private final Map<ValidationProfile, ValidationService> cache;

    public ValidationServiceFactory(ValidationThresholds defaultThresholds) {
        this.cache = new EnumMap<>(ValidationProfile.class);

        // Pre-build all profiles. STANDARD uses the injected default thresholds
        // (which may come from application.yml); the others use fixed presets.
        for (ValidationProfile profile : ValidationProfile.values()) {
            ValidationThresholds thresholds = (profile == ValidationProfile.STANDARD)
                    ? defaultThresholds
                    : ValidationThresholdProfileFactory.process(profile);
            cache.put(profile, new ValidationService(thresholds));
        }
    }

    /**
     * Returns the cached {@link ValidationService} for the given profile.
     * Defaults to {@link ValidationProfile#STANDARD} when {@code null}.
     */
    public ValidationService forProfile(ValidationProfile profile) {
        return cache.getOrDefault(
                profile == null ? ValidationProfile.STANDARD : profile,
                cache.get(ValidationProfile.STANDARD)
        );
    }
}
