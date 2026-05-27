# red-dragon Review

Last updated: 2026-05-27

This is the repo-wide current work-remains summary. Module review files contain
the detailed local context; [ISSUES.md](ISSUES.md) is the task index.

## Current Open Work

| Module | Open work | Source |
| --- | --- | --- |
| `lib-ingestion` | Future firehose work needs a CIK-to-tickers map. SEC body parsing for Form 4, 13D/G, offering docs, XBRL, latest-filings RSS, and the LLM ticker scheduler remain deferred. | [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |
| `lib-marketdata` | Schwab retry/backoff should distinguish retryable HTTP failures from caller/programmer errors and add jittered exponential backoff. VWAP needs an explicit session-boundary API. Yahoo provider and remaining snapshot builders need coverage, and thresholds should be revalidated after ATR/volatility math changes. | [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| `lib-analytics` | Add integration/equivalence tests proving orchestrator output matches the underlying scorers and standalone classifier. Calibration threshold configuration remains a design question. | [lib-analytics/REVIEW.md](lib-analytics/REVIEW.md) |
| `lib-validation` | Deployment-threshold semantics need design decisions: gate/scoring order, separate concentration-input thresholds, and whether deployment confidence should constrain STANDARD/PROBE tiers. Threshold profile binding and profile tests are also open. | [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| `lib-domain` | Value-object construction still has determinism gaps because several constructors default timestamps with `Instant.now()`. Some closed sets are still represented as strings instead of enums, and score-validation policy is only partially unified. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |
| `lib-math` | Duplicate helper implementations remain across score/math utility classes. Package layout and ambiguous zero-weight `weightedAverage` behavior still need a decision. | [lib-math/REVIEW.md](lib-math/REVIEW.md) |
| `lib-persistence` | Repository/migration integration coverage is thin, long text columns can truncate/reject large notes, and generated-id entities still expose positional all-args construction in some places. Historical plaintext Schwab token audit rows must be re-saved or purged if retroactive cleanup is required. | [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |
| `lib-backtest` | Determinism, verdict distribution, average-score tests, and app wiring verification are still needed. | [lib-backtest/REVIEW.md](lib-backtest/REVIEW.md) |
| `lib-execution` | Broker execution remains design-only. Account reads, position reads, order construction, placement, cancellation, fills, and dry-run/live controls are not implemented. | [lib-execution/SCHWAB_EXECUTION.md](lib-execution/SCHWAB_EXECUTION.md) |

## Current Shape

The pipeline is functional as a candidate-review system: `app` can run manual
and SEC-sourced candidates through market-data enrichment, analytics,
validation, review, persistence, calibration, and backtest surfaces. The
implemented system stops before broker execution.

Most major early audit findings have been fixed: SEC HTTP hardening, 8-K
taxonomy coverage, ticker filtering, manual candidate idempotency, shared ATR
math, Wilder ATR, realized-volatility definitions, analytics orchestrator
delegation, scorer-owned notes, validation OBSERVE reachability, persistence
relationship/audit migrations, validation-reason normalization, optimistic
locking, Schwab token encryption, backtest model immutability, and shared
production/backtest validation-input construction.

The remaining work is less about broad missing scaffolding and more about
hardening boundaries: configuration binding, deterministic construction,
historical secret cleanup, and tests that protect behavior across module
boundaries.
