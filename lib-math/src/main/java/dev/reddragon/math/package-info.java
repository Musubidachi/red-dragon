/**
 * Shared numeric helpers for clamping, normalized-score validation,
 * weighted averages, and percent-change math.
 *
 * <p>These helpers are upstream of {@code lib-analytics} (which must remain
 * deterministic and side-effect free), so every helper here is also a pure
 * function with no I/O, no state, and no clock dependency.
 *
 * <p>Two flavors of bounds enforcement live here:
 * <ul>
 *   <li>{@link dev.reddragon.math.AnalyticsScoreUtils#clamp(double) clamp} —
 *       coerces an in-range value, rejects {@code NaN}.</li>
 *   <li>{@link dev.reddragon.math.ValidationScoreUtils#requireNormalized(String, double) requireNormalized}
 *       — throws on any out-of-range value, including {@code NaN}.</li>
 * </ul>
 */
package dev.reddragon.math;
