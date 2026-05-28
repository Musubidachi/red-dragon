package dev.reddragon.analytics.services;

import dev.reddragon.analytics.services.classification.DirectionalPersistenceScorer;
import dev.reddragon.analytics.services.classification.EquilibriumQualityScorer;
import dev.reddragon.analytics.services.classification.LiquidityTextureScorer;
import dev.reddragon.analytics.services.classification.OptionsFlowScorer;
import dev.reddragon.analytics.services.classification.RegimeCompatibilityResult;
import dev.reddragon.analytics.services.classification.RegimeCompatibilityScorer;
import dev.reddragon.analytics.services.classification.VolatilityExpansionScorer;
import dev.reddragon.analytics.services.classification.VwapInteractionScorer;
import dev.reddragon.analytics.services.deployment.DeploymentConfidenceScorer;
import dev.reddragon.analytics.services.propagation.NarrativeExpansionScorer;
import dev.reddragon.analytics.services.propagation.ReflexivityScorer;
import dev.reddragon.analytics.services.propagation.SectorPropagationScorer;
import dev.reddragon.analytics.services.structural.AsymmetryScorer;
import dev.reddragon.analytics.services.structural.DilutionRiskScorer;
import dev.reddragon.analytics.services.structural.MaterialityImpactScorer;
import dev.reddragon.domain.models.RegimeLabel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScorerNoteIsolationTest {

    @Test
    void noteEmittingScorersDoNotAcceptCallerOwnedNoteLists() {
        List<Class<?>> scorers = List.of(
                DirectionalPersistenceScorer.class,
                EquilibriumQualityScorer.class,
                LiquidityTextureScorer.class,
                OptionsFlowScorer.class,
                RegimeCompatibilityScorer.class,
                VolatilityExpansionScorer.class,
                VwapInteractionScorer.class,
                DeploymentConfidenceScorer.class,
                NarrativeExpansionScorer.class,
                ReflexivityScorer.class,
                SectorPropagationScorer.class,
                AsymmetryScorer.class,
                DilutionRiskScorer.class,
                MaterialityImpactScorer.class
        );

        for (Class<?> scorer : scorers) {
            for (Method method : scorer.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && method.getName().equals("process")) {
                    boolean acceptsNoteList = Arrays.stream(method.getParameterTypes())
                            .anyMatch(List.class::isAssignableFrom);
                    assertFalse(
                            acceptsNoteList,
                            scorer.getSimpleName() + "#process must return notes instead of accepting a mutable note list"
                    );
                }
            }
        }
    }

    @Test
    void scoreResultDefensivelyCopiesNotes() {
        List<String> callerOwnedNotes = new ArrayList<>(List.of("first"));

        ScoreResult result = new ScoreResult(0.50, callerOwnedNotes);
        callerOwnedNotes.add("caller mutation");

        assertEquals(List.of("first"), result.notes());
        assertThrows(UnsupportedOperationException.class, () -> result.notes().add("result mutation"));
    }

    @Test
    void regimeCompatibilityResultDefensivelyCopiesNotes() {
        List<String> callerOwnedNotes = new ArrayList<>(List.of("regime note"));

        RegimeCompatibilityResult result = new RegimeCompatibilityResult(RegimeLabel.MIXED, callerOwnedNotes);
        callerOwnedNotes.add("caller mutation");

        assertEquals(List.of("regime note"), result.notes());
        assertThrows(UnsupportedOperationException.class, () -> result.notes().add("result mutation"));
    }
}
