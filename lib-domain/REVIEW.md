# lib-domain Review

Last updated: 2026-05-27

`lib-domain` has no module-local README; the root README and architecture docs
are the canonical module spec. This review keeps the current open work separate
from fixed historical audit findings.

## Current Open Work

No open module-local issues are currently tracked.

Add focused constructor and validation tests as new shared models gain callers.

## Implemented Surface

Implemented today:

* Shared candidate, market-data, analytics, validation, calibration, adversarial,
  trade-modification, and exit value objects.
* Enums for catalyst type, source type, market quality, liquidity tier,
  score dimension, regime, verdict, deployment tier, reason code, risk flag,
  calibration drift, and exit recommendation.
* Builder support for `CandidateValidationInput`.
* Shared `OhlcBar` contract used by market-data calculations.
* Snapshot pair javadocs that explain provider-context snapshots versus
  score-only scorer snapshots.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| `TradeCandidate` validation | Delegates normalized-score validation to shared `ValidationScoreUtils`. |
| Unified score policy | `DomainScorePolicy` documents strict input/validation-output scores versus clamped derived scores; domain value objects now route normalized score handling through that policy. |
| Constructor timestamps | Snapshot, quote, outcome, candidate, and trade-modification constructors now require explicit instants instead of defaulting to wall-clock time. `MarketQuote.unavailable(symbol, note)` remains a convenience factory; deterministic callers can use its explicit timestamp overload. |
| Closed score/tier sets | `ScoreDimension` and `LiquidityTier` replace string returns for strongest/weakest/dominant score dimensions and market liquidity tiers. |
| Accumulator bugs | Strongest/weakest/dominant dimension helpers now update both name and accumulator consistently. |
| Market snapshot validation | Prices, volumes, ATR, gaps, VWAP deviation, and relative volume now reject invalid values appropriately. |
| Relative volume flooring | `MarketMathUtils.floorAtZero` prevents `Math.max` from hiding NaN behavior. |
| Regime labels | `AnalyticsSnapshot.regimeLabel` no longer silently defaults to MIXED. |
| Utility style | `IngestionTextUtils` follows the utility-class convention and documents its cross-module domain role. |
| Snapshot naming | Javadocs clarify similarly named market-state snapshot pairs. |
| Domain threshold ownership | Liquidity tiers and hostile-market predicates are documented as fixed domain predicates. Tunable admission, scoring, and deployment thresholds remain in validation or analytics configuration. |
| Value-object test breadth | Focused tests now cover high-risk score-only snapshots, market-context snapshots, candidate validation inputs, analytics snapshots, quote validation, trade modification requests, adversarial findings, and validation summaries. |

## Notes

Because this module is shared by almost every other module, open work here
should usually be done with narrow migrations and caller-by-caller tests.

Root tracking: [../ISSUES.md](../ISSUES.md).
