/**
 * Shared numeric helpers for clamping, normalized-score validation,
 * weighted averages, and percent-change math.
 *
 * <p>The public API is intentionally flat under {@code dev.reddragon.math}.
 * This module is already the utility boundary, so adding a {@code utilities}
 * subpackage would add package stutter without separating another domain.
 *
 * <p>These helpers are upstream of {@code lib-analytics} (which must remain
 * deterministic and side-effect free), so every helper here is also a pure
 * function with no I/O, no state, and no clock dependency.
 *
 * <p>{@code weightedAverage} helpers return {@code 0.0} when
 * {@code totalWeight == 0.0}. In red-dragon this is a "no signal" sentinel;
 * callers that need to distinguish missing weighted inputs from a real zero
 * score should check the weight before calling.
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
