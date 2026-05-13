/**
 * <h2>MD Layer 8 — Meta-System Adaptation Layer</h2>
 *
 * The framework looking at itself: is the edge still working, are
 * equilibrium assumptions still valid, is propagation happening faster
 * than it used to? This is where we detect drift in our own scoring.
 *
 * <p><b>For a junior developer:</b> these classes consume realized outcomes
 * (from closed trades) or live context (current market state) and emit
 * diagnostics about the system itself — not about any individual candidate.
 *
 * <p>Members:
 * <ul>
 *   <li>{@link dev.reddragon.analytics.meta.LiveContextAdaptationAnalyzer}
 *       — adapts active context (e.g. dampens scores when regime is hostile)</li>
 *   <li>{@link dev.reddragon.analytics.meta.LongHorizonCalibrationAnalyzer}
 *       — measures whether scoring has drifted from historical performance
 *       on a window of realized outcomes</li>
 * </ul>
 */
package dev.reddragon.analytics.meta;
