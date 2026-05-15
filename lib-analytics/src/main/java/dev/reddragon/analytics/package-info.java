/**
 * Analytics: market-state classification, asymmetry scoring, structural
 * validation, propagation tracking, deployment confidence, exit timing,
 * and meta-system adaptation.
 *
 * <h2>Layout</h2>
 *
 * Like every module, this one follows the type-first layout
 * (models/, services/, utilities/, config/). The MD-layer groupings live
 * one level inside {@code services/}:
 *
 * <ul>
 *   <li>{@link dev.reddragon.analytics.services.structural}    - MD Layer 3
 *       Structural Validation (is the catalyst real, material, meaningful)</li>
 *   <li>{@link dev.reddragon.analytics.services.classification} - MD Layer 4
 *       Market-State Classification (what regime are we in)</li>
 *   <li>{@link dev.reddragon.analytics.services.deployment}    - MD Layer 5
 *       Deployment confidence input (how aggressive should we size)</li>
 *   <li>{@link dev.reddragon.analytics.services.propagation}   - MD Layer 6
 *       Narrative Propagation Monitoring (is the story spreading)</li>
 *   <li>{@link dev.reddragon.analytics.services.exit}          - MD Layer 7
 *       Exit / Equilibrium Compression (when to hold, scale, exit)</li>
 *   <li>{@link dev.reddragon.analytics.services.meta}          - MD Layer 8
 *       Meta-System Adaptation (is our edge degrading)</li>
 *   <li>{@link dev.reddragon.analytics.services} (top-level)   - Orchestrators
 *       that blend the above layers into single snapshots</li>
 *   <li>{@link dev.reddragon.analytics.models}                 - Shared value
 *       objects (snapshots, labels, breakdowns)</li>
 *   <li>{@link dev.reddragon.analytics.utilities}              - Pure math helpers</li>
 * </ul>
 *
 * <p>For a junior developer: everything here is a pure function. No I/O,
 * no portfolio state, no order routing. Each scorer takes a snapshot in
 * and returns a 0.0-1.0 score out. The orchestrators in services/ compose
 * the layers; lib-validation then combines the analytics output with hard
 * rules into a single trade verdict.
 */
package dev.reddragon.analytics;
