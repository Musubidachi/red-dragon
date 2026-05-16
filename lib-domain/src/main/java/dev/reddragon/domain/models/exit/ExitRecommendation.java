package dev.reddragon.domain.models.exit;

/**
 * Trader-facing exit action recommendation for an open position.
 *
 * <p>The L7 (Exit / Equilibrium Compression) layer never closes positions
 * automatically. It only suggests what the trader's <i>next</i> action should
 * be given how asymmetry, equilibrium, and propagation have evolved since
 * entry.
 */
public enum ExitRecommendation {

    /** Conditions still support the original thesis. Stay in the trade. */
    HOLD,

    /** Asymmetry is compressing. Move stops up; reduce risk without exiting. */
    TIGHTEN,

    /** Take partial profits — the easy part of the move is likely behind us. */
    SCALE_OUT,

    /** Asymmetry, equilibrium, and propagation all argue the move is over. Exit. */
    EXIT_NOW
}
