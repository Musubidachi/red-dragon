/**
 * <h2>MD Layer 5 — Deployment Engine</h2>
 *
 * Sizing and aggressiveness signals: given that we like this candidate,
 * how confidently should we deploy capital into it?
 *
 * <p><b>For a junior developer:</b> the final tier decision (CONCENTRATED,
 * STANDARD, PROBE, OBSERVE, NONE) is owned by lib-validation's
 * {@link dev.reddragon.validation.engine.DeploymentResolver}. The scorer in
 * this package supplies one of its inputs — a 0.0–1.0 confidence number.
 *
 * <p>Members:
 * <ul>
 *   <li>{@link dev.reddragon.analytics.deployment.DeploymentConfidenceScorer}
 *       — overall deployment confidence based on structural, asymmetry,
 *       and regime signals</li>
 * </ul>
 */
package dev.reddragon.analytics.deployment;
