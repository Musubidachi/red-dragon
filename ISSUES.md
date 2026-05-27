# red-dragon Issue Index

Last updated: 2026-05-27

This index tracks current actionable documentation-known work. Fixed historical
findings are intentionally omitted unless they leave a follow-up.

## High Priority

No open high-priority issues are currently tracked.

## Remaining Issues By Fix Location

Use this section to pick work by the area that would receive the primary code
or documentation change. Priority is retained for sequencing.

### `lib-ingestion`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M1 | Medium | Open | Build CIK-to-tickers support for future firehose ingestion | Future CIK-first paths need share-class-aware ticker emission. | [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |
| RD-M2 | Medium | Deferred | Implement deferred SEC body and feed parsing | Form 4, 13D/G, offerings, XBRL, RSS firehose, and LLM scheduler docs remain design-forward. | [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |
| RD-L1 | Low | Open | Inject shared Jackson `ObjectMapper` in SEC services | Local mapper construction makes global Jackson config harder to apply. | [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |

### `lib-marketdata`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M3 | Medium | Open | Harden Schwab retry/backoff | Current retry behavior can retry non-retryable errors and lacks jittered exponential backoff. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| RD-M4 | Medium | Open | Define VWAP session boundary API | Passing multi-session intraday bars can silently produce cumulative VWAP. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| RD-M5 | Medium | Open | Finish marketdata provider and snapshot-builder coverage | Yahoo and several builders remain weakly covered; math changes need calibration validation. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| RD-L3 | Low | Open | Decide calculator service versus utility style | Stateless calculators still read as services; this is mostly consistency work. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |

### `lib-analytics`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M6 | Medium | Open | Add analytics orchestrator/scorer equivalence tests | Future scorer/orchestrator drift could pass boundary-only tests. | [lib-analytics/REVIEW.md](lib-analytics/REVIEW.md) |

### `lib-validation`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M7 | Medium | Open | Decide validation gate/scoring order | Current engine computes factor scores even when hard gates already reject; docs imply gates come first. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-M8 | Medium | Open | Split concentration input thresholds from aggregate pass threshold | Tuning pass threshold also changes concentration asymmetry/earlyness requirements unintentionally. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-M9 | Medium | Open | Define deployment confidence role for STANDARD and PROBE | A low deployment-confidence candidate can still receive STANDARD if the aggregate score passes. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-M10 | Medium | Open | Add threshold configuration binding | Profile docs imply YAML-driven tuning, but hardcoded defaults remain the local factory source. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-L4 | Low | Open | Refactor hard-gate marker and subservice construction | Current marker logic and manual subservice construction are brittle but not urgent. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-L5 | Low | Open | Add validation profile tests | New profiles could violate ordering invariants without direct coverage. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |

### `lib-domain`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M11 | Medium | Open | Remove constructor-time `Instant.now()` defaults | Identical logical inputs can construct non-equal value objects, weakening backtest determinism. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |
| RD-M12 | Medium | Open | Replace stringly typed dimensions and tiers with enums | Runtime string comparisons can silently miss typos in closed sets. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |
| RD-L6 | Low | Open | Rename or document cross-module `IngestionTextUtils` | The name suggests module ownership even though the helper is shared. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |

### `lib-math`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M13 | Medium | Open | Consolidate duplicated numeric helpers | Future fixes to clamp/average/weighted-average behavior must be repeated across classes. | [lib-math/REVIEW.md](lib-math/REVIEW.md) |
| RD-L7 | Low | Open | Decide `lib-math` package layout and zero-weight docs | The module does not follow the `utilities/` convention and `weightedAverage` zero behavior is ambiguous. | [lib-math/REVIEW.md](lib-math/REVIEW.md) |

### `lib-persistence`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M14 | Medium | Open | Expand persistence integration tests | Entity, repository, migration, and constraint behavior are under-tested. | [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |
| RD-M15 | Medium | Open | Address truncation-prone text storage | Long notes, explanations, or import warnings can be rejected or truncated depending on DB behavior. | [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |
| RD-L8 | Low | Open | Finish builder migration for generated-id entities | Positional constructors still expose generated IDs on less-used entities. | [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |

### `lib-backtest`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M16 | Medium | Open | Add backtest determinism and metric tests | The module's main guarantee is not fully pinned by tests. | [lib-backtest/REVIEW.md](lib-backtest/REVIEW.md) |

### `app`

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-L9 | Low | Open | Verify app-side backtest bean wiring | The module is fixed locally, but app wiring should be confirmed where it is consumed. | [lib-backtest/REVIEW.md](lib-backtest/REVIEW.md) |

### New `lib-execution` Module

| ID | Priority | Status | Title | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M17 | Medium | Deferred | Promote broker execution from design to module when ready | No broker account, position, order, cancellation, or fill lifecycle exists in the Maven reactor. | [lib-execution/SCHWAB_EXECUTION.md](lib-execution/SCHWAB_EXECUTION.md) |
