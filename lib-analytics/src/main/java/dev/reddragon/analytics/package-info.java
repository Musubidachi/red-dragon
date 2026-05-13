/**
 * Analytics: market-state classification, asymmetry scoring, structural
 * validation, propagation tracking, deployment confidence, and meta-system
 * adaptation.
 *
 * <h2>MD-layer mapping</h2>
 *
 * This module implements five of the eight layers from the trading framework
 * specification. Each layer lives in its own sub-package so the mapping is
 * directly visible in code:
 *
 * <ul>
 *   <li>{@link dev.reddragon.analytics.structural}    — <b>MD Layer 3</b>
 *       Structural Validation (is the catalyst real, material, and meaningful)</li>
 *   <li>{@link dev.reddragon.analytics.classification} — <b>MD Layer 4</b>
 *       Market-State Classification (what regime are we in)</li>
 *   <li>{@link dev.reddragon.analytics.deployment}    — <b>MD Layer 5</b>
 *       Deployment confidence input (how aggressive should we size)</li>
 *   <li>{@link dev.reddragon.analytics.propagation}   — <b>MD Layer 6</b>
 *       Narrative Propagation Monitoring (is the story spreading or saturated)</li>
 *   <li>{@link dev.reddragon.analytics.meta}          — <b>MD Layer 8</b>
 *       Meta-System Adaptation (is our edge degrading)</li>
 *   <li>{@link dev.reddragon.analytics.service}       — Orchestrators that
 *       blend the above layers into single snapshots</li>
 *   <li>{@link dev.reddragon.analytics.model}         — Shared value objects
 *       (snapshots, labels, breakdowns)</li>
 *   <li>{@link dev.reddragon.analytics.util}          — Pure math helpers</li>
 * </ul>
 *
 * <p><b>For a junior developer:</b> everything here is a pure function. No I/O,
 * no portfolio state, no order routing. Each scorer takes a snapshot in and
 * returns a 0.0–1.0 score out. The orchestrator in {@code service/} composes
 * the layers; {@code lib-validation} then combines the analytics output with
 * hard rules into a single trade verdict.
 */
package dev.reddragon.analytics;
