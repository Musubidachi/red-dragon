# lib-math Review

Last updated: 2026-05-26

This review summarizes the small shared numeric helper module after the local
NaN/finite-value fixes. The remaining work is mostly consolidation and API
semantics.

## Current Open Work

| Priority | Issue | Status | Impact | Next action |
| --- | --- | --- | --- | --- |
| Medium | Duplicate helper implementations | Open | `clamp`, `average`, and `weightedAverage` behavior exists in multiple utility classes. | Consolidate common operations or document why separate namespaces are intentional. |
| Low | Package layout versus `utilities/` convention | Open | The module's helpers live directly under `dev.reddragon.math`, not a `utilities` subpackage. | Either move classes with import migration or document `lib-math` as an exception. |
| Low | `weightedAverage` zero-weight ambiguity | Open | Returning `0.0` for zero total weight can mean either "no signal" or a real zero score. | Document the sentinel or introduce a stricter/optional variant for callers that need distinction. |

## Implemented Surface

Implemented today:

* `AnalyticsScoreUtils` for score clamping, averaging, and weighted averages.
* `MarketMathUtils` for clamping, percent change, averages, and floor-at-zero.
* `ValidationScoreUtils` for strict normalized and non-negative validation plus
  weighted averages.
* Tests for NaN/infinity rejection, double assertion deltas, and helper
  boundary behavior.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| NaN clamp behavior | `AnalyticsScoreUtils.clamp` and `MarketMathUtils.clamp` reject NaN instead of returning it. |
| Percent change | `safePercentChange` rejects non-finite, non-positive previous values and non-finite current values. |
| Strict score validation | `requireNormalized` rejects NaN; `requireNonNegative` rejects NaN and positive infinity. |
| Module description | POM/package docs now include clamping, normalized-score validation, weighted averages, and percent-change math. |
| Tests | Double assertions use explicit deltas and cover NaN/infinity guard paths. |
| New helper | `MarketMathUtils.floorAtZero` was added for callers that need non-negative finite flooring. |

Root tracking: [../ISSUES.md](../ISSUES.md).
