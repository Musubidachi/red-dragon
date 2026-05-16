/**
 * <h2>MD Layer 6 — Narrative Propagation Monitoring</h2>
 *
 * Tracks how a story is spreading: how fast, how broadly, how reflexively.
 * Used to gauge whether asymmetry is compressing (narrative saturated)
 * or expanding (narrative still early).
 *
 * <p><b>For a junior developer:</b> "propagation" here is the speed and
 * breadth of attention — not whether the news is true. Truth-checking
 * lives in the structural layer.
 *
 * <p>Members:
 * <ul>
 *   <li>{@link dev.reddragon.analytics.services.propagation.NarrativeExpansionScorer}
 *       — narrative breadth and coherence growth</li>
 *   <li>{@link dev.reddragon.analytics.services.propagation.PropagationPhaseAnalyzer}
 *       — labels propagation as early / accelerating / saturated</li>
 *   <li>{@link dev.reddragon.analytics.services.propagation.ReflexivityScorer}
 *       — feedback-loop intensity between price and attention</li>
 *   <li>{@link dev.reddragon.analytics.services.propagation.SectorPropagationScorer}
 *       — sympathy moves in the same sector</li>
 * </ul>
 */
package dev.reddragon.analytics.services.propagation;
