package dev.reddragon.analytics.services.exit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.reddragon.analytics.models.PhaseLabel;
import dev.reddragon.analytics.models.exit.ExitRecommendation;
import dev.reddragon.analytics.models.exit.ExitSignal;
import dev.reddragon.analytics.models.exit.ExitSignalInput;
import dev.reddragon.analytics.utilities.AnalyticsScoreUtils;

/**
 * <h2>MD Layer 7 — Exit and Equilibrium Compression Engine</h2>
 *
 * Detects when equilibrium restoration is complete, propagation has saturated,
 * or asymmetry has materially compressed since entry. Emits an
 * {@link ExitSignal} the trader can use to decide whether to hold, tighten
 * stops, scale out, or exit.
 *
 * <p>The scoring is intentionally explicit. There is no machine learning here.
 * Each signal contributes to the {@code compressionScore} additively, and the
 * recommendation falls out of three thresholds:
 *
 * <ul>
 *   <li>{@code &gt;= 0.75} → {@link ExitRecommendation#EXIT_NOW}</li>
 *   <li>{@code &gt;= 0.55} → {@link ExitRecommendation#SCALE_OUT}</li>
 *   <li>{@code &gt;= 0.35} → {@link ExitRecommendation#TIGHTEN}</li>
 *   <li>otherwise         → {@link ExitRecommendation#HOLD}</li>
 * </ul>
 *
 * <p>Inputs come from earlier layers: equilibrium phase (L4), propagation
 * phase (L6), and the current vs. entry asymmetry (L3).
 */
public class EquilibriumCompressionScorer {

    private static final double EXIT_THRESHOLD      = 0.75;
    private static final double SCALE_OUT_THRESHOLD = 0.55;
    private static final double TIGHTEN_THRESHOLD   = 0.35;

    /**
     * Main processing flow. Pure function — same input always yields the
     * same {@link ExitSignal}.
     */
    public ExitSignal process(ExitSignalInput input) {
        Objects.requireNonNull(input, "input is required");

        List<String> notes = new ArrayList<>();
        double compression = 0.0;

        compression += asymmetryCompressionContribution(input, notes);
        compression += propagationContribution(input.propagationPhase(), notes);
        compression += equilibriumContribution(input.equilibriumPhase(), notes);
        compression += rangeExtensionContribution(input, notes);

        double finalScore = AnalyticsScoreUtils.clamp(compression);
        ExitRecommendation recommendation = recommendation(finalScore);
        notes.add("Recommendation: " + recommendation.name() + " (compression=" + format(finalScore) + ").");

        return new ExitSignal(recommendation, finalScore, notes);
    }

    /**
     * The dominant signal: how much asymmetry has been consumed since entry.
     * A drop of more than 50% from entry contributes near-fully; a small drop
     * contributes little.
     *
     * <p>Caps out at 0.50 of the total compression score so a single dimension
     * cannot drive an exit on its own.
     */
    private double asymmetryCompressionContribution(ExitSignalInput input, List<String> notes) {
        double entry = Math.max(0.01, input.entryAsymmetry());
        double current = input.currentAsymmetry();
        double ratio = Math.max(0.0, Math.min(1.0, current / entry));
        // ratio = 1.0 → no consumption (0 contribution); ratio = 0.0 → fully consumed (0.50 contribution)
        double contribution = (1.0 - ratio) * 0.50;
        if (contribution >= 0.20) {
            notes.add("Asymmetry has compressed from " + format(entry) + " to " + format(current)
                    + " (" + format(1.0 - ratio) + " of original edge consumed).");
        }
        return contribution;
    }

    /**
     * Propagation phase contributes up to 0.25.
     *
     * <p>Early phases ({@code EARLY_DISCOVERY}, {@code EMERGING_PROPAGATION})
     * contribute nothing — the narrative is still expanding. Late phases
     * ({@code LATE_REFLEXIVITY}, {@code SATURATION_RISK}, {@code EXHAUSTION})
     * contribute their full weight.
     */
    private double propagationContribution(PhaseLabel propagationPhase, List<String> notes) {
        return switch (propagationPhase) {
            case UNKNOWN, EARLY_DISCOVERY, EMERGING_PROPAGATION -> 0.00;
            case EARLY_REFLEXIVITY -> 0.05;
            case BROAD_ACCELERATION -> {
                notes.add("Propagation is in broad acceleration — narrative tailwind likely peaking.");
                yield 0.10;
            }
            case LATE_REFLEXIVITY -> {
                notes.add("Propagation has entered late reflexivity — remaining upside is thinning.");
                yield 0.18;
            }
            case SATURATION_RISK -> {
                notes.add("Propagation has reached saturation risk — reflexive expansion is unlikely.");
                yield 0.25;
            }
            case ASYMMETRY_DECAY -> {
                notes.add("Asymmetry is actively decaying within the propagation cycle.");
                yield 0.22;
            }
            case EQUILIBRIUM_BREAKDOWN -> {
                notes.add("Equilibrium assumptions are breaking down inside the propagation cycle.");
                yield 0.25;
            }
            case EXHAUSTION -> {
                notes.add("Propagation has exhausted — narrative momentum is spent.");
                yield 0.25;
            }
        };
    }

    /**
     * Equilibrium phase contributes up to 0.15.
     *
     * <p>The phase enum is shared with propagation but interpreted differently:
     * here we care whether the equilibrium-restoration assumption underneath
     * the trade is still valid.
     */
    private double equilibriumContribution(PhaseLabel equilibriumPhase, List<String> notes) {
        return switch (equilibriumPhase) {
            case UNKNOWN, EARLY_DISCOVERY, EMERGING_PROPAGATION -> 0.00;
            case EARLY_REFLEXIVITY, BROAD_ACCELERATION -> 0.03;
            case LATE_REFLEXIVITY -> 0.07;
            case SATURATION_RISK -> {
                notes.add("Equilibrium is saturated — restoration is essentially complete.");
                yield 0.12;
            }
            case ASYMMETRY_DECAY -> {
                notes.add("Equilibrium-restoration asymmetry is decaying.");
                yield 0.13;
            }
            case EQUILIBRIUM_BREAKDOWN -> {
                notes.add("Equilibrium has broken down — the original thesis assumption is weakening.");
                yield 0.15;
            }
            case EXHAUSTION -> {
                notes.add("Equilibrium-restoration potential is exhausted.");
                yield 0.15;
            }
        };
    }

    /** Price extension contributes up to 0.10. */
    private double rangeExtensionContribution(ExitSignalInput input, List<String> notes) {
        if (input.nearRecentHigh() && input.rangePosition() > 0.90) {
            notes.add("Price is at the top of its recent range — remaining upside is compressed.");
            return 0.10;
        }
        if (input.rangePosition() > 0.85) {
            notes.add("Price is extended within its recent range.");
            return 0.05;
        }
        return 0.00;
    }

    private ExitRecommendation recommendation(double compression) {
        if (compression >= EXIT_THRESHOLD)      return ExitRecommendation.EXIT_NOW;
        if (compression >= SCALE_OUT_THRESHOLD) return ExitRecommendation.SCALE_OUT;
        if (compression >= TIGHTEN_THRESHOLD)   return ExitRecommendation.TIGHTEN;
        return ExitRecommendation.HOLD;
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
