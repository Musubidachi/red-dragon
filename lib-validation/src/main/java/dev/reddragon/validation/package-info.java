/**
 * Validation: combine hard rules and analytics scores into a single trade-candidate verdict.
 *
 * <p>Two-stage pipeline:
 * <ol>
 *   <li><b>Hard rules</b> — deterministic gates that reject before any scoring is wasted.
 *       Examples: minimum dollar volume / float, options chain availability, IRA-allowed
 *       instrument types, ticker not on a personal blocklist, candidate not stale.</li>
 *   <li><b>Score aggregation</b> — combines the regime-fit score and asymmetry score
 *       from {@code lib-analytics} into a final verdict (PASS / WATCH / REJECT) with
 *       the full reasoning chain attached so review remains auditable.</li>
 * </ol>
 *
 * <p>This module does <i>not</i> know about a current portfolio. Position-aware sizing
 * and execution belong to a future module that only gets built if/when execution is in scope.
 */
package dev.reddragon.validation;
