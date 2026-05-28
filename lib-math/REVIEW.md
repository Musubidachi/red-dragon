# lib-math Review

Last updated: 2026-05-27

This review summarizes the small shared numeric helper module after the local
NaN/finite-value fixes and the RD-M13/RD-L7 cleanup.

## Current Open Work

No open lib-math review items remain from RD-M13/RD-L7.

## Implemented Surface

Implemented today:

* `AnalyticsScoreUtils` for score clamping, averaging, and weighted averages.
* `MarketMathUtils` for clamping, percent change, averages, and floor-at-zero.
* `ValidationScoreUtils` for strict normalized and non-negative validation plus
  weighted averages.
* Package-private `CoreMathUtils` as the single implementation for shared
  clamp, average, and weighted-average behavior.
* Tests for NaN/infinity rejection, double assertion deltas, and helper
  boundary behavior.
* `lib-math/README.md` and package docs clarifying the flat package layout and
  zero-weight weighted-average sentinel.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| NaN clamp behavior | `AnalyticsScoreUtils.clamp` and `MarketMathUtils.clamp` reject NaN instead of returning it. |
| Duplicate helpers | Public facades delegate duplicated clamp, average, and weighted-average arithmetic to `CoreMathUtils`. |
| Package layout | `dev.reddragon.math` is documented as the intentional public package root; no `utilities` subpackage migration is planned. |
| Zero-weight weighted average | `0.0` is documented and tested as the no-signal sentinel when total weight is zero. |
| Percent change | `safePercentChange` rejects non-finite, non-positive previous values and non-finite current values. |
| Strict score validation | `requireNormalized` rejects NaN; `requireNonNegative` rejects NaN and positive infinity. |
| Module description | POM/package docs now include clamping, normalized-score validation, weighted averages, and percent-change math. |
| Tests | Double assertions use explicit deltas and cover NaN/infinity guard paths. |
| New helper | `MarketMathUtils.floorAtZero` was added for callers that need non-negative finite flooring. |

Root tracking: [../ISSUES.md](../ISSUES.md).
