/**
 * <h2>MD Layer 3 — Structural Validation Engine</h2>
 *
 * Scorers and analyzers that answer the question "is this catalyst real,
 * meaningful, and material?" — the second-pass filter that runs once a
 * candidate has been surfaced by ingestion.
 *
 * <p><b>For a junior developer:</b> classes here are pure functions. They
 * take snapshots in (no IO, no state), return a 0.0–1.0 score or a list
 * of findings out. They never call each other; they are aggregated by
 * the higher-level orchestrator in {@link dev.reddragon.analytics.service}.
 *
 * <p>Members:
 * <ul>
 *   <li>{@link dev.reddragon.analytics.structural.AdversarialValidationAnalyzer}
 *       — finds contradictions in the bullish thesis</li>
 *   <li>{@link dev.reddragon.analytics.structural.AsymmetryScorer}
 *       — measures remaining risk/reward asymmetry</li>
 *   <li>{@link dev.reddragon.analytics.structural.DilutionRiskScorer}
 *       — scores dilution / share-issuance risk</li>
 *   <li>{@link dev.reddragon.analytics.structural.MaterialityImpactScorer}
 *       — scores how materially the catalyst affects the company</li>
 * </ul>
 */
package dev.reddragon.analytics.structural;
