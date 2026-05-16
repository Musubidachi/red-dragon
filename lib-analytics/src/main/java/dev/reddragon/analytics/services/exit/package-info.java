/**
 * <h2>MD Layer 7 — Exit and Equilibrium Compression Engine</h2>
 *
 * Decides when an open position should be held, tightened, scaled out of, or
 * exited based on how asymmetry, equilibrium phase, and propagation phase
 * have evolved since entry.
 *
 * <p><b>For a junior developer:</b> Layers 3–6 score candidates <i>before</i>
 * a position exists. This layer scores the trade <i>after</i> it exists. The
 * inputs come from the upstream layers (asymmetry from L3, equilibrium phase
 * from L4, propagation phase from L6) plus a small amount of state — entry
 * asymmetry and current range position.
 *
 * <p>Members:
 * <ul>
 *   <li>{@link dev.reddragon.analytics.services.exit.EquilibriumCompressionScorer}
 *       — composes signals into an {@link dev.reddragon.domain.models.exit.ExitSignal}</li>
 *   <li>{@link dev.reddragon.domain.models.exit.ExitSignal} — output: recommendation
 *       + compression score + ordered notes</li>
 *   <li>{@link dev.reddragon.domain.models.exit.ExitSignalInput} — value object the
 *       trader supplies via {@code POST /api/exit-signal}</li>
 *   <li>{@link dev.reddragon.domain.models.exit.ExitRecommendation} — HOLD / TIGHTEN
 *       / SCALE_OUT / EXIT_NOW</li>
 * </ul>
 */
package dev.reddragon.analytics.services.exit;
