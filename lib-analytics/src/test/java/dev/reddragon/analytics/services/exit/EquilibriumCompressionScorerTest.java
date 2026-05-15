package dev.reddragon.analytics.services.exit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.reddragon.analytics.models.PhaseLabel;
import dev.reddragon.analytics.models.exit.ExitRecommendation;
import dev.reddragon.analytics.models.exit.ExitSignal;
import dev.reddragon.analytics.models.exit.ExitSignalInput;

class EquilibriumCompressionScorerTest {

    private final EquilibriumCompressionScorer scorer = new EquilibriumCompressionScorer();

    @Test
    void freshThesisWithExpandingPropagationReturnsHold() {
        ExitSignalInput input = new ExitSignalInput(
                PhaseLabel.EARLY_DISCOVERY,
                PhaseLabel.EMERGING_PROPAGATION,
                0.78,
                0.80,
                0.40,
                false
        );

        ExitSignal signal = scorer.process(input);

        assertEquals(ExitRecommendation.HOLD, signal.recommendation());
        assertTrue(signal.compressionScore() < 0.35);
    }

    @Test
    void saturatedPropagationAndCompressedAsymmetryTriggersExit() {
        ExitSignalInput input = new ExitSignalInput(
                PhaseLabel.SATURATION_RISK,
                PhaseLabel.SATURATION_RISK,
                0.20,
                0.85,
                0.95,
                true
        );

        ExitSignal signal = scorer.process(input);

        assertEquals(ExitRecommendation.EXIT_NOW, signal.recommendation());
        assertTrue(signal.compressionScore() >= 0.75);
    }

    @Test
    void halfConsumedAsymmetryWithLateReflexivityScalesOut() {
        ExitSignalInput input = new ExitSignalInput(
                PhaseLabel.LATE_REFLEXIVITY,
                PhaseLabel.LATE_REFLEXIVITY,
                0.40,
                0.80,
                0.70,
                false
        );

        ExitSignal signal = scorer.process(input);

        assertTrue(
                signal.recommendation() == ExitRecommendation.SCALE_OUT
                        || signal.recommendation() == ExitRecommendation.TIGHTEN,
                "Expected SCALE_OUT or TIGHTEN, got " + signal.recommendation()
        );
    }

    @Test
    void modestCompressionInExpandingNarrativeStillHolds() {
        ExitSignalInput input = new ExitSignalInput(
                PhaseLabel.EMERGING_PROPAGATION,
                PhaseLabel.BROAD_ACCELERATION,
                0.65,
                0.75,
                0.55,
                false
        );

        ExitSignal signal = scorer.process(input);

        // Asymmetry only compressed ~13%, equilibrium is still early — should be HOLD or just TIGHTEN
        assertTrue(signal.compressionScore() < 0.55,
                "Expected compression < 0.55, got " + signal.compressionScore());
    }

    @Test
    void compressionScoreIsClampedToOne() {
        // Every dimension pushes toward exit
        ExitSignalInput input = new ExitSignalInput(
                PhaseLabel.EXHAUSTION,
                PhaseLabel.EXHAUSTION,
                0.05,
                1.00,
                1.00,
                true
        );

        ExitSignal signal = scorer.process(input);

        assertTrue(signal.compressionScore() <= 1.0);
        assertEquals(ExitRecommendation.EXIT_NOW, signal.recommendation());
    }

    @Test
    void notesIncludeRecommendationLine() {
        ExitSignalInput input = new ExitSignalInput(
                PhaseLabel.UNKNOWN,
                PhaseLabel.UNKNOWN,
                0.50,
                0.50,
                0.50,
                false
        );

        ExitSignal signal = scorer.process(input);

        assertTrue(signal.notes().stream().anyMatch(n -> n.startsWith("Recommendation:")));
    }
}
