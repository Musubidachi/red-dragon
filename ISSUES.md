# red-dragon Issue Index

Last updated: 2026-05-26

This index tracks current actionable documentation-known work. Fixed historical
findings are intentionally omitted unless they leave a follow-up.

## High Priority

| ID | Title | Module | Status | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-H2 | Replace mutable scorer note side effects | `lib-analytics` | Open | Scorers mutate caller-owned lists, weakening the pure-function contract and note provenance. | [lib-analytics/REVIEW.md](lib-analytics/REVIEW.md) |
| RD-H3 | Share validation-input construction between production and backtest | `lib-backtest` | Open | Backtest and live pipeline can drift because risk-threshold literals are duplicated. | [lib-backtest/REVIEW.md](lib-backtest/REVIEW.md) |
| RD-H4 | Protect persisted Schwab tokens | `lib-persistence` | Open | Plain database token storage can expose brokerage credentials to anyone with DB read access. | [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |

## Medium Priority

| ID | Title | Module | Status | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-M1 | Build CIK-to-tickers support for future firehose ingestion | `lib-ingestion` | Open | Future CIK-first paths need share-class-aware ticker emission. | [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |
| RD-M2 | Implement deferred SEC body and feed parsing | `lib-ingestion` | Deferred | Form 4, 13D/G, offerings, XBRL, RSS firehose, and LLM scheduler docs remain design-forward. | [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |
| RD-M3 | Harden Schwab retry/backoff | `lib-marketdata` | Open | Current retry behavior can retry non-retryable errors and lacks jittered exponential backoff. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| RD-M4 | Define VWAP session boundary API | `lib-marketdata` | Open | Passing multi-session intraday bars can silently produce cumulative VWAP. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| RD-M5 | Finish marketdata provider and snapshot-builder coverage | `lib-marketdata` | Open | Yahoo and several builders remain weakly covered; math changes need calibration validation. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| RD-M6 | Add analytics orchestrator/scorer equivalence tests | `lib-analytics` | Open | Future scorer/orchestrator drift could pass boundary-only tests. | [lib-analytics/REVIEW.md](lib-analytics/REVIEW.md) |
| RD-M7 | Decide validation gate/scoring order | `lib-validation` | Open | Current engine computes factor scores even when hard gates already reject; docs imply gates come first. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-M8 | Split concentration input thresholds from aggregate pass threshold | `lib-validation` | Open | Tuning pass threshold also changes concentration asymmetry/earlyness requirements unintentionally. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-M9 | Define deployment confidence role for STANDARD and PROBE | `lib-validation` | Open | A low deployment-confidence candidate can still receive STANDARD if the aggregate score passes. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-M10 | Add threshold configuration binding | `lib-validation` | Open | Profile docs imply YAML-driven tuning, but hardcoded defaults remain the local factory source. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-M11 | Remove constructor-time `Instant.now()` defaults | `lib-domain` | Open | Identical logical inputs can construct non-equal value objects, weakening backtest determinism. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |
| RD-M12 | Replace stringly typed dimensions and tiers with enums | `lib-domain` | Open | Runtime string comparisons can silently miss typos in closed sets. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |
| RD-M13 | Consolidate duplicated numeric helpers | `lib-math` | Open | Future fixes to clamp/average/weighted-average behavior must be repeated across classes. | [lib-math/REVIEW.md](lib-math/REVIEW.md) |
| RD-M14 | Expand persistence integration tests | `lib-persistence` | Open | Entity, repository, migration, and constraint behavior are under-tested. | [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |
| RD-M15 | Address truncation-prone text storage | `lib-persistence` | Open | Long notes, explanations, or import warnings can be rejected or truncated depending on DB behavior. | [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |
| RD-M16 | Add backtest determinism and metric tests | `lib-backtest` | Open | The module's main guarantee is not fully pinned by tests. | [lib-backtest/REVIEW.md](lib-backtest/REVIEW.md) |
| RD-M17 | Promote broker execution from design to module when ready | `lib-execution` | Deferred | No broker account, position, order, cancellation, or fill lifecycle exists in the Maven reactor. | [lib-execution/SCHWAB_EXECUTION.md](lib-execution/SCHWAB_EXECUTION.md) |

## Low Priority

| ID | Title | Module | Status | Impact | Source |
| --- | --- | --- | --- | --- | --- |
| RD-L1 | Inject shared Jackson `ObjectMapper` in SEC services | `lib-ingestion` | Open | Local mapper construction makes global Jackson config harder to apply. | [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |
| RD-L3 | Decide calculator service versus utility style | `lib-marketdata` | Open | Stateless calculators still read as services; this is mostly consistency work. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| RD-L4 | Refactor hard-gate marker and subservice construction | `lib-validation` | Open | Current marker logic and manual subservice construction are brittle but not urgent. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-L5 | Add validation profile tests | `lib-validation` | Open | New profiles could violate ordering invariants without direct coverage. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| RD-L6 | Rename or document cross-module `IngestionTextUtils` | `lib-domain` | Open | The name suggests module ownership even though the helper is shared. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |
| RD-L7 | Decide `lib-math` package layout and zero-weight docs | `lib-math` | Open | The module does not follow the `utilities/` convention and `weightedAverage` zero behavior is ambiguous. | [lib-math/REVIEW.md](lib-math/REVIEW.md) |
| RD-L8 | Finish builder migration for generated-id entities | `lib-persistence` | Open | Positional constructors still expose generated IDs on less-used entities. | [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |
| RD-L9 | Verify app-side backtest bean wiring | `lib-backtest` | Open | The module is fixed locally, but app wiring should be confirmed where it is consumed. | [lib-backtest/REVIEW.md](lib-backtest/REVIEW.md) |
