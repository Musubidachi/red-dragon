# lib-math

Shared numeric helpers used by the internal red-dragon libraries.

## Package Layout

Production code intentionally lives directly under `dev.reddragon.math`.
`lib-math` is already the utility boundary, so a `utilities` subpackage would
only add package stutter. Keep new public helpers in this package unless they
belong to a clearly separate domain.

The public facade classes are:

* `AnalyticsScoreUtils` for normalized analytics score clamping, averages, and
  weighted averages.
* `MarketMathUtils` for market-data normalization, percent changes, averages,
  and non-negative flooring.
* `ValidationScoreUtils` for strict normalized score and weight validation plus
  weighted averages.

Common primitive behavior is centralized in package-private `CoreMathUtils` so
the public API remains stable while clamp, average, and weighted-average
semantics stay single-sourced.

## Weighted Averages

`weightedAverage(weightedTotal, totalWeight)` returns `0.0` when
`totalWeight == 0.0`. That value is a no-signal sentinel, not proof of an
observed zero score. Callers that need to distinguish "no weighted inputs" from
a real zero score should inspect `totalWeight` before calling.

For validation flows, weights should be checked with
`ValidationScoreUtils.requireNonNegative(...)` before aggregation.
