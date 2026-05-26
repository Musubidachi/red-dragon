# lib-domain Review

Last updated: 2026-05-26

`lib-domain` has no module-local README; the root README and architecture docs
are the canonical module spec. This review keeps the current open work separate
from fixed historical audit findings.

## Current Open Work

| Priority | Issue | Status | Impact | Next action |
| --- | --- | --- | --- | --- |
| High | Constructor-time `Instant.now()` defaults | Open | Identical logical inputs can construct non-equal value objects, weakening backtest determinism and cache/dedup behavior. | Require explicit timestamps or move "now" behavior into factories with injected clocks. |
| Medium | Score-validation strategy is only partially unified | Open | Some objects clamp while others throw; `TradeCandidate` was fixed, but the cross-class policy still needs to be documented and applied. | Define clamp-versus-throw policy by semantic category and update constructors consistently. |
| Medium | Stringly typed dimensions and tiers | Open | `liquidityTier`, strongest/weakest dimensions, and dominant scores expose strings for closed sets. | Add `LiquidityTier` and `ScoreDimension` enums and migrate callers. |
| Medium | Domain-level magic thresholds | Open | Helpers such as `hasCredibleStructuralCatalyst` and `isHostile` embed thresholds outside validation config. | Move thresholds to validation/analytics config or document these as fixed domain predicates. |
| Low | Shared helper naming | Open | `IngestionTextUtils` lives in shared domain utilities but is named as if it belongs only to ingestion. | Rename or document the cross-module role. |
| Low | Value-object test breadth | Open | Tests cover representative objects, not the full shared model surface. | Add focused constructor/validation tests for the highest-traffic value objects. |

## Implemented Surface

Implemented today:

* Shared candidate, market-data, analytics, validation, calibration, adversarial,
  trade-modification, and exit value objects.
* Enums for catalyst type, source type, market quality, regime, verdict,
  deployment tier, reason code, risk flag, calibration drift, and exit
  recommendation.
* Builder support for `CandidateValidationInput`.
* Shared `OhlcBar` contract used by market-data calculations.
* Snapshot pair javadocs that explain provider-context snapshots versus
  score-only scorer snapshots.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| `TradeCandidate` validation | Delegates normalized-score validation to shared `ValidationScoreUtils`. |
| Accumulator bugs | Strongest/weakest/dominant dimension helpers now update both name and accumulator consistently. |
| Market snapshot validation | Prices, volumes, ATR, gaps, VWAP deviation, and relative volume now reject invalid values appropriately. |
| Relative volume flooring | `MarketMathUtils.floorAtZero` prevents `Math.max` from hiding NaN behavior. |
| Regime labels | `AnalyticsSnapshot.regimeLabel` no longer silently defaults to MIXED. |
| Utility style | `IngestionTextUtils` follows the utility-class convention. |
| Snapshot naming | Javadocs clarify similarly named market-state snapshot pairs. |

## Notes

Because this module is shared by almost every other module, open work here
should usually be done with narrow migrations and caller-by-caller tests.

Root tracking: [../ISSUES.md](../ISSUES.md).
