/**
 * Analytics: market-state (regime) classification and asymmetry scoring of
 * candidate trades.
 *
 * <p>Two responsibilities:
 * <ul>
 *   <li><b>Regime classifier</b> — given current market features, returns the
 *       governing state (rotational equilibrium, directional expansion,
 *       informational expansion, volatility compression / expansion, liquidity
 *       deterioration, macro instability, news-driven shock).</li>
 *   <li><b>Asymmetry scorer</b> — given an enriched candidate (catalyst type,
 *       freshness, capital-flow signal, fragility risk), returns a probabilistic
 *       score plus the contributing factors so the decision is auditable.</li>
 * </ul>
 *
 * <p>Pure functions over inputs. No I/O, no portfolio state, no order routing.
 * Outputs are consumed by {@code lib-validation} which combines them with hard
 * rules into a single verdict.
 */
package dev.reddragon.analytics;
