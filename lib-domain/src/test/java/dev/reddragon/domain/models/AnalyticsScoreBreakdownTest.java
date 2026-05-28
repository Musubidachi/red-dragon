package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyticsScoreBreakdownTest {

    @Test
    void strongestAndWeakestDimensionsReturnScoreDimension() {
        AnalyticsScoreBreakdown scores = new AnalyticsScoreBreakdown(
                0.4, 0.2, 0.3, 0.8, 0.6, 0.7, 0.5, 0.9);

        assertEquals(ScoreDimension.DEPLOYMENT_CONFIDENCE, scores.strongestDimension());
        assertEquals(ScoreDimension.MATERIAL_SIGNIFICANCE, scores.weakestDimension());
    }

    @Test
    void scoreDimensionConvertsFromExistingStringKeys() {
        assertEquals(ScoreDimension.STRUCTURAL_REALITY, ScoreDimension.fromKey("structuralReality"));
        assertEquals(ScoreDimension.REFLEXIVITY_POTENTIAL, ScoreDimension.fromKey("reflexivityPotential"));
        assertThrows(IllegalArgumentException.class, () -> ScoreDimension.fromKey("unknown"));
    }
}
