package dev.reddragon.domain.models;

/**
 * Closed set of analytics score dimensions exposed by domain helper methods.
 */
public enum ScoreDimension {
    STRUCTURAL_REALITY("structuralReality"),
    MATERIAL_SIGNIFICANCE("materialSignificance"),
    EARLYNESS("earlyness"),
    EQUILIBRIUM_QUALITY("equilibriumQuality"),
    REFLEXIVITY_POTENTIAL("reflexivityPotential"),
    ASYMMETRY("asymmetry"),
    REGIME_COMPATIBILITY("regimeCompatibility"),
    DEPLOYMENT_CONFIDENCE("deploymentConfidence");

    private final String key;

    ScoreDimension(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static ScoreDimension fromKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("score dimension key is required");
        }
        String normalized = key.trim();
        for (ScoreDimension dimension : values()) {
            if (dimension.key.equals(normalized)) {
                return dimension;
            }
        }
        throw new IllegalArgumentException("Unknown score dimension: " + key);
    }
}
