/**
 * Validation: combine hard rules and analytics scores into a single
 * trade-candidate verdict.
 *
 * <h2>MD-layer mapping</h2>
 *
 * This module is the top of <b>MD Layer 3 (Structural Validation Engine)</b>,
 * and it owns <b>MD Layer 5 (Deployment Engine)</b> tier assignment.
 *
 * <p>Two-stage pipeline inside {@link dev.reddragon.validation.services.engine}:
 * <ol>
 *   <li><b>Hard gates</b> — deterministic rejections evaluated by
 *       {@link dev.reddragon.validation.services.engine.HardGateEvaluator} (minimum
 *       liquidity, IRA-allowed instruments, blocklist, staleness, etc.).
 *       Fail fast before scoring spends cycles.</li>
 *   <li><b>Score aggregation</b> —
 *       {@link dev.reddragon.validation.services.engine.DisequilibriumValidationEngine}
 *       blends the analytics layer's seven dimensions into a final PASS /
 *       WATCH / REJECT verdict via
 *       {@link dev.reddragon.validation.services.engine.VerdictResolver}, then
 *       {@link dev.reddragon.validation.services.engine.DeploymentResolver} assigns
 *       a deployment tier (CONCENTRATED, STANDARD, PROBE, OBSERVE, NONE).</li>
 * </ol>
 *
 * <p><b>For a junior developer:</b> this module does <i>not</i> know about a
 * current portfolio. Position-aware sizing and order routing live in the
 * (still-doc-only) {@code lib-execution} module, behind a dry-run gate.
 */
package dev.reddragon.validation;
