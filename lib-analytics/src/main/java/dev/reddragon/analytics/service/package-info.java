/**
 * Analytics orchestration: top-level services that compose the per-layer
 * scorers from sibling packages into a single snapshot.
 *
 * <p>These are the only classes outside this module that other modules need
 * to know about. They form the stable API of {@code lib-analytics}.
 *
 * <ul>
 *   <li>{@link dev.reddragon.analytics.service.DeterministicAnalyticsService}
 *       — main pipeline entry point. Takes a candidate + market snapshot and
 *       emits an {@link dev.reddragon.analytics.model.AnalyticsSnapshot}.
 *       Blends MD Layers 3–6.</li>
 *   <li>{@link dev.reddragon.analytics.service.MarketStateClassifier}
 *       — standalone rule-based classifier exposed via the
 *       {@code /market-state} HTTP endpoint. MD Layer 4.</li>
 * </ul>
 *
 * <p><b>For a junior developer:</b> if you are adding a new scorer, put the
 * scorer itself in the layer sub-package it belongs to
 * ({@code structural}, {@code classification}, {@code propagation},
 * {@code deployment}, {@code meta}) — not here. Only the orchestrators that
 * combine multiple layers live in {@code service}.
 */
package dev.reddragon.analytics.service;
