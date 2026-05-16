package dev.reddragon.domain.models.exit;

import dev.reddragon.domain.models.PhaseLabel;
import dev.reddragon.math.AnalyticsScoreUtils;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Inputs the L7 (Exit / Equilibrium Compression) layer needs to decide
 * whether to hold, tighten, scale out, or exit an open position.
 *
 * <p>All score fields are normalized to {@code [0.0, 1.0]} by the constructor.
 *
 * <p><b>For a junior developer:</b> this is what we know <i>right now</i> about
 * the trade — what asymmetry was at entry, what it is now, where price is in
 * its range, and what the equilibrium / propagation phases look like.
 */
@Value
@Accessors(fluent = true)
public class ExitSignalInput {

    /** Equilibrium phase from {@code EquilibriumPhaseAnalyzer} (L4). */
    PhaseLabel equilibriumPhase;

    /** Propagation phase from {@code PropagationPhaseAnalyzer} (L6). */
    PhaseLabel propagationPhase;

    /** Current asymmetry score from {@code AsymmetryScorer} (L3), 0.0–1.0. */
    double currentAsymmetry;

    /** Asymmetry at the time of entry, 0.0–1.0. Used to detect compression. */
    double entryAsymmetry;

    /** Where price sits in the recent range, 0.0 (low) – 1.0 (high). */
    double rangePosition;

    /** Whether price is sitting at / near the recent high. */
    boolean nearRecentHigh;

    public ExitSignalInput(
            PhaseLabel equilibriumPhase,
            PhaseLabel propagationPhase,
            double currentAsymmetry,
            double entryAsymmetry,
            double rangePosition,
            boolean nearRecentHigh
    ) {
        this.equilibriumPhase = Objects.requireNonNull(equilibriumPhase, "equilibriumPhase is required");
        this.propagationPhase = Objects.requireNonNull(propagationPhase, "propagationPhase is required");
        this.currentAsymmetry = AnalyticsScoreUtils.clamp(currentAsymmetry);
        this.entryAsymmetry = AnalyticsScoreUtils.clamp(entryAsymmetry);
        this.rangePosition = AnalyticsScoreUtils.clamp(rangePosition);
        this.nearRecentHigh = nearRecentHigh;
    }
}
