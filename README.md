# red-dragon

Probabilistic market-state intelligence platform.

## What This Is

A trade-candidate pipeline. Ideas flow in from external sources such as SEC
EDGAR filings or manual entry, get enriched with market-data context, scored
against market regime and asymmetry signals, then filtered by a validation
layer. What survives is presented to the trader with the reasoning chain
attached.

The system is candidate-in / verdict-out. It does not place orders or track a
live portfolio. It stores candidate history, validation results, trader notes,
calibration outcomes, imported trade-history samples, market observations, and
Schwab OAuth state so decisions can be reviewed and calibrated later. It
augments discretionary decisions; it does not replace them.

Only `app` produces a bootable Spring Boot artifact. All Maven reactor modules
under `lib-*` are plain jars consumed by `app`. `lib-execution` is design-only
and is not listed in the parent Maven reactor.

## Pipeline

```text
[ ingestion ]  SEC filings, manual candidates
     |
     v
[ enrichment ] market-data features (VWAP, ATR, range position, liquidity, gap)
     |
     v
[ analytics ]  regime classifier + asymmetry scorer
     |
     v
[ validation ] hard gates + score aggregation -> PASS / WATCH / REJECT + deployment tier
     |
     v
[ review ]     trader sees survivors with full reasoning chain and risk flags
```

## Module Status

| Module | Current role | Implemented today | Still needed | Details |
| --- | --- | --- | --- | --- |
| `app` | Wires the platform and exposes HTTP endpoints. | Spring Boot main, controllers, pipeline orchestration, scheduler/status surfaces, calibration/history/backtest/review endpoints, Schwab OAuth controllers. It is the only bootable artifact. | Keep controller docs verified against source before expanding this README; continue to keep business logic in libraries. | [ARCHITECTURE.md](ARCHITECTURE.md) |
| `lib-math` | Shared numeric helper jar. | Clamping, normalized-score validation, non-negative guards, weighted averages, `safePercentChange`, `floorAtZero`, and tests for NaN/infinity handling. | Consolidate duplicate clamp/average/weighted-average helpers; decide whether to move helpers under a `utilities` package; document `weightedAverage` zero-weight behavior. | [lib-math/REVIEW.md](lib-math/REVIEW.md) |
| `lib-domain` | Shared cross-module value objects and enums. | Candidates, market bars, market snapshots, analytics snapshots, validation inputs/results, verdicts, risk flags, calibration reports, exit DTOs, and builder support for `CandidateValidationInput`. | Remove constructor-time `Instant.now()` defaults, replace string dimension/tier names with enums, finish score-validation strategy cleanup, and broaden value-object tests. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |
| `lib-ingestion` | L1 opportunity discovery and candidate-side L2 ingestion. | Manual candidate ingestion; SEC submissions client by CIK or ticker; ticker-to-CIK lookup with TTL/fail-stale refresh; SEC HTTP timeouts/retry/rate limiting; 8-K item taxonomy; form variant handling; CIK format utilities; ticker filtering; deterministic manual IDs. | Build CIK-to-tickers support for future firehose paths and implement deferred Form 4 / 13D-G / offering / XBRL body parsing. | [lib-ingestion/README.md](lib-ingestion/README.md), [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |
| `lib-marketdata` | Market-data provider adapters and deterministic feature derivation. | Schwab, Yahoo, Noop, and composite providers; daily/intraday bars and quotes; ATR via shared OHLC contract; Wilder ATR; realized volatility; VWAP and feature calculators; replay/stream helpers; analytics-owned market score enrichment. | Harden Schwab retry selection/backoff, define VWAP session boundaries, finish Yahoo/snapshot-builder coverage, and revalidate calibration thresholds after ATR/volatility math changes. | [lib-marketdata/README.md](lib-marketdata/README.md), [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| `lib-analytics` | Pure deterministic scorers and analytics orchestration. | Layered L3-L8 scorers, `DeterministicAnalyticsService`, standalone market-state classifier, exit-signal scorer, calibration drift analyzer, and market-data snapshot scoring. The orchestrator delegates to scorer classes. | Replace mutable note-list side effects with explicit result objects, add orchestrator/scorer equivalence integration tests, and decide whether calibration thresholds should become config-bound. | [lib-analytics/README.md](lib-analytics/README.md), [lib-analytics/REVIEW.md](lib-analytics/REVIEW.md) |
| `lib-validation` | Hard gates, aggregate verdicts, and deployment tiers. | Validation service facade, gate evaluator, factor factory, confidence scorer, verdict resolver, deployment resolver, risk flags, summary formatter, threshold profiles, and OBSERVE-threshold support. | Decide gate/scoring order, split concentration input thresholds from aggregate pass thresholds, define how deployment confidence affects STANDARD/PROBE tiers, add configuration binding for thresholds, and add profile tests. | [lib-validation/README.md](lib-validation/README.md), [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| `lib-persistence` | JPA entities, repositories, mapper, and Flyway schema. | Runtime migrations through V14, 15 tables including normalized validation reasons, optimistic locking on mutable tables, audit timestamps, backtest FK, validation-verdict idempotency key, and trade-history storage. | Encrypt or otherwise protect Schwab tokens, expand repository/migration integration tests, address truncation-prone text columns, and finish builder migration for generated-id entities. | [lib-persistence/README.md](lib-persistence/README.md), [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |
| `lib-backtest` | Deterministic replay harness supporting L8 calibration. | `BacktestFrame`, `BacktestOutcome`, `BacktestMetrics`, `BacktestReport`, and stateless `BacktestReplayEngine` running marketdata -> analytics -> validation. | Extract shared production/backtest `CandidateValidationInput` construction, migrate to builder construction, add determinism and metric tests, and verify app bean wiring. | [lib-backtest/README.md](lib-backtest/README.md), [lib-backtest/REVIEW.md](lib-backtest/REVIEW.md) |
| `lib-execution` | Planned broker execution layer. | Design documentation only. Schwab OAuth support exists in `app` and `lib-persistence`; Schwab market data exists in `lib-marketdata`. | Create a real Maven module only when broker account reads, position reads, order construction, order placement, cancellation, fill reconciliation, and dry-run/live controls are implemented. | [lib-execution/SCHWAB_EXECUTION.md](lib-execution/SCHWAB_EXECUTION.md) |

See [REVIEW.md](REVIEW.md) for the current repo-wide work-remains summary,
[ISSUES.md](ISSUES.md) for the issue index, and
[MARKDOWN_CLEANUP_GUIDE.md](MARKDOWN_CLEANUP_GUIDE.md) for future doc cleanup
rules.

## Package Layout

Most modules follow this shape:

```text
<module>/src/main/java/dev/reddragon/<module>/
    models/      value objects, DTOs, snapshots, records, enums
    domains/     JPA entities (lib-persistence only)
    services/    business logic
    controllers/ HTTP controllers (app only)
    utilities/   pure static helpers
    config/      Spring configuration and properties classes
```

`lib-domain` owns shared domain language under `dev.reddragon.domain.models`.
`lib-math` owns shared numeric helpers under `dev.reddragon.math`. Module-owned
wire DTOs, controller DTOs, provider DTOs, and JPA entities intentionally stay
in their owning modules.

## Project Conventions

* Lombok is the default for value objects, service constructors, and loggers.
* No nested classes. Every class, enum, record, and interface lives in its own
  top-level file.
* Shared domain models live in `lib-domain`; shared numeric helpers live in
  `lib-math`.
* `lib-analytics` is deterministic and side-effect free.
* Schwab provider DTOs stay inside `lib-marketdata/services/provider/schwab/`.

## HTTP API Surface

This table is a working index of the current API. Before adding endpoints here,
verify paths against controllers in `app`; controller source is the runtime
truth.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/` | Static review dashboard. |
| GET | `/health` | Liveness check. |
| GET | `/actuator/metrics` | Micrometer metrics. |
| POST | `/api/pipeline/manual` | Run a manually supplied candidate through the full pipeline. |
| GET | `/api/pipeline/sec/{cik}` | Fetch SEC filings for a CIK and run them through the pipeline. |
| GET | `/api/pipeline/sec/ticker/{ticker}` | Resolve ticker to CIK, fetch SEC filings, and run them through the pipeline. |
| GET | `/api/pipeline/sec/watch-list` | Run configured CIKs through the SEC pipeline. |
| GET | `/api/pipeline/status` | Pipeline and scheduler status. |
| GET | `/api/analysis/{ticker}` | Analyze one ticker using market data, SEC lookup, deterministic rules, optional LLM web research, and source-coverage confidence. |
| POST | `/api/review/manual` | Ad-hoc manual review without persistence. |
| GET | `/api/review/candidates` | Recent PASS / WATCH verdicts with reasoning. |
| GET | `/api/candidates` | Stored candidates. |
| GET | `/api/candidates/{symbol}/history` | Candidate history for one symbol. |
| GET | `/api/candidates/id/{candidateId}` | Candidate lookup by id. |
| POST | `/api/candidates/{id}/notes` | Add a trader note to a candidate. |
| GET | `/api/candidates/{id}/notes` | List trader notes for a candidate. |
| GET | `/api/stats` | General pipeline stats. |
| GET | `/api/stats/opportunity-quality` | Conviction mix, deployment-tier mix, and top symbols. |
| POST | `/api/backtest` | Run a replay against historical frames. |
| GET | `/api/backtest/results/{runId}` | Stored backtest results for a run. |
| GET | `/api/backtest/runs` | Stored backtest run ids. |
| GET | `/api/market-data/{symbol}/daily` | Historical daily bars from the configured provider chain. |
| GET | `/api/market-data/{symbol}/daily/stored` | Stored daily bars for a symbol/date range. |
| GET | `/api/market-data/{symbol}/intraday` | Intraday bars from the configured provider chain. |
| GET | `/api/market-data/{symbol}/intraday/stored` | Stored intraday bars for a symbol/time range. |
| GET | `/api/market-data/{symbol}/quote` | Latest quote from the configured provider chain. |
| GET | `/api/market-data/{symbol}/quote/stored` | Stored quote observations for a symbol. |
| POST | `/api/market-state/classify` | Classify a supplied market-state snapshot. |
| POST | `/market-structure/intraday` | Derive an intraday structure snapshot from bars. |
| POST | `/api/exit-signal` | L7 exit signal: HOLD / TIGHTEN / SCALE_OUT / EXIT_NOW. |
| POST | `/api/calibration` | Append realized outcomes and return a drift report. |
| GET | `/api/calibration` | Current calibration drift report. |
| GET | `/api/calibration/summary` | Rolling summary: win rate, average return, average drawdown. |
| GET | `/api/calibration/outcomes` | Recent outcome rows. |
| GET | `/api/calibration/outcomes/export` | CSV export of recent outcomes. |
| GET | `/api/calibration/outcomes/{symbol}` | Recent outcomes for one symbol. |
| DELETE | `/api/calibration/outcomes` | Delete all calibration outcomes. |
| DELETE | `/api/calibration/outcomes/{symbol}` | Delete calibration outcomes for one symbol. |
| POST | `/api/history/import` | Import and normalize trade-history CSV text. |
| GET | `/api/history/imports` | Recent trade-history import batches. |
| GET | `/api/history/imports/{batchId}` | One import batch with normalized records. |
| GET | `/api/history/trades` | Stored normalized trade-history records, optionally filtered by ticker. |
| GET | `/api/schwab/oauth/authorize-url` | URL the trader visits to grant Schwab API access. |
| GET | `/api/schwab/oauth/callback` | Exchange Schwab OAuth code and persist tokens. |
| POST | `/api/schwab/oauth/refresh` | Manually refresh Schwab access token. |
| GET | `/api/regime/history` | Stored regime / analytics history. |
| GET | `/api/verdicts/{candidateId}/summary` | Display-ready validation summary. |
| POST | `/api/verdicts/{id}/override` | Record a manual verdict override. |
| GET | `/api/verdicts/stats` | Verdict distribution stats. |
| GET | `/api/validation/profiles` | Available validation threshold profiles. |

Calibration also exposes convenience summary endpoints under
`/api/calibration/summary/**` for win rate, returns, drawdown, medians, holding
days, and per-symbol variants.
