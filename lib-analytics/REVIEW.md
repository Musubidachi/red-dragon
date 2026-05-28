# lib-analytics Review

Last updated: 2026-05-27

This review summarizes the current analytics module after the orchestrator was
converted to use the documented scorer classes. Older audit detail is
compressed so current risks are visible first.

## Current Open Work

No open module-local issues are currently tracked.

## Implemented Surface

Implemented today:

* `DeterministicAnalyticsService` as the main analytics pipeline entry.
* `MarketStateClassifier` for standalone regime classification.
* L3 structural scorers, L4 classification scorers, L5 deployment-confidence
  input, L6 propagation/reflexivity scorers, L7 exit signal scorer, and L8
  calibration analyzers.
* Market-data snapshot scoring for liquidity and volatility stability after the
  marketdata/analytics boundary was clarified.
* Null-tolerant adversarial analysis for richer optional snapshots.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| Orchestrator/scorer drift | `DeterministicAnalyticsService` delegates to scorer classes instead of duplicating most scoring inline. |
| Orchestrator/scorer test coverage | `DeterministicAnalyticsServiceEquivalenceTest` verifies snapshot scores and scorer notes against the production scorer pipeline, plus shared-label alignment with the standalone classifier. |
| Regime coverage | Standalone classifier and orchestrator use the same regime labels, including hostile-news and supportive-compression cases. |
| Deterministic timestamp | Analytics snapshots use market-data observation time instead of wall-clock time. |
| Dependency wiring | Scorers are constructor-provided instead of being hidden `new` fields in the orchestrator. |
| Locale stability | Formatted exit notes use `Locale.ROOT`. |
| Exhaustive enums | Regime score mapping no longer hides future enum additions behind a default branch. |
| Adversarial duplication | The orchestrator now calls the richer analyzer and keeps only clearly named lightweight pipeline-only checks. |
| L7 design | Architecture docs explain why exit recommendations are advisory and do not use the L5 resolver split. |
| Scorer note side effects | Note-emitting scorers return immutable score/regime result objects with scorer-owned notes, and the orchestrator composes those notes locally. |
| Calibration threshold ownership | Calibration drift thresholds are analytics-owned in `CalibrationDriftThresholds`; app wiring binds them from `red-dragon.analytics.calibration.*`. Validation thresholds remain limited to candidate admission and deployment-tier decisions. |

## Deferred Design Notes

`META_ADAPTATION_FEEDBACK.md` remains partly forward-looking. The implemented
L8 surface is the current `LongHorizonCalibrationAnalyzer` and related app
calibration service, not the full feedback-governance system described in the
design doc.

Root tracking: [../ISSUES.md](../ISSUES.md).
