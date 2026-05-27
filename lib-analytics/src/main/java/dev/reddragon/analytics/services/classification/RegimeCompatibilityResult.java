package dev.reddragon.analytics.services.classification;

import dev.reddragon.domain.models.RegimeLabel;

import java.util.List;
import java.util.Objects;

/**
 * Immutable regime classification plus scorer-owned narrative notes.
 */
public final class RegimeCompatibilityResult {

    private final RegimeLabel regimeLabel;
    private final List<String> notes;

    public RegimeCompatibilityResult(RegimeLabel regimeLabel, List<String> notes) {
        this.regimeLabel = Objects.requireNonNull(regimeLabel, "regimeLabel is required");
        this.notes = List.copyOf(notes == null ? List.of() : notes);
    }

    public RegimeLabel regimeLabel() {
        return regimeLabel;
    }

    public List<String> notes() {
        return notes;
    }
}
