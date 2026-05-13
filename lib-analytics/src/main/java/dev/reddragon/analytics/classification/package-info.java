/**
 * <h2>MD Layer 4 — Market-State Classification Engine</h2>
 *
 * Scorers that answer "what kind of market are we in right now, and is it
 * supportive of equilibrium-restoration trading?" This is the regime layer.
 *
 * <p><b>For a junior developer:</b> nothing here predicts price direction.
 * Each class measures one observable property of the current environment
 * (VWAP behavior, liquidity texture, volatility expansion, etc.) and
 * returns a normalized 0.0–1.0 score. The orchestrator in
 * {@link dev.reddragon.analytics.service} blends these into a regime label.
 *
 * <p>Members:
 * <ul>
 *   <li>{@link dev.reddragon.analytics.classification.EquilibriumQualityScorer}
 *       — how well-behaved is mean reversion right now</li>
 *   <li>{@link dev.reddragon.analytics.classification.EquilibriumPhaseAnalyzer}
 *       — labels the current intraday phase (used by Layer 7 exit logic too)</li>
 *   <li>{@link dev.reddragon.analytics.classification.DirectionalPersistenceScorer}
 *       — how trendy / persistent is the move</li>
 *   <li>{@link dev.reddragon.analytics.classification.VolatilityExpansionScorer}
 *       — is realized volatility expanding</li>
 *   <li>{@link dev.reddragon.analytics.classification.VwapInteractionScorer}
 *       — how price is interacting with VWAP</li>
 *   <li>{@link dev.reddragon.analytics.classification.LiquidityTextureScorer}
 *       — depth, spread, and participation quality</li>
 *   <li>{@link dev.reddragon.analytics.classification.OptionsFlowScorer}
 *       — unusual options activity intensity</li>
 *   <li>{@link dev.reddragon.analytics.classification.RegimeCompatibilityScorer}
 *       — top-level regime label (rotational / trending / hostile)</li>
 * </ul>
 */
package dev.reddragon.analytics.classification;
