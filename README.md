# red-dragon

Probabilistic market-state intelligence platform.

## What This Is

A trade-candidate pipeline. Ideas flow in from external sources (SEC EDGAR
filings, manual entry), get enriched with market-data context, scored against
the current market regime and an asymmetry rubric, then filtered by a
seven-dimension validation layer. What survives is presented to the trader for
review with a full reasoning chain attached.

The system is candidate-in / verdict-out. It does not place orders or track a
live portfolio. It does store candidate history, validation results, trader
notes, calibration outcomes, and imported trade-history samples so decisions can
be reviewed and calibrated later. It augments discretionary decisions; it does
not replace them.

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

## Module Layout

| Module | MD layers | Purpose |
| --- | --- | --- |
| `app` | wires all | Only deployable artifact. Spring Boot main, web layer, pipeline wiring. |
| `lib-math` | shared | Numeric helpers for clamping, normalized-score validation, weighted averages, and percent-change math. |
| `lib-domain` | shared | Shared value objects and enums that flow across modules: candidates, market snapshots, analytics snapshots, validation results, and verdict types. |
| `lib-ingestion` | L1, L2 candidates | Pull candidate inputs: SEC EDGAR filings and manual entry. |
| `lib-marketdata` | L2 market data | Market-data provider adapters and VWAP/ATR/range/liquidity feature derivation. |
| `lib-analytics` | L3, L4, L5, L6, L7, L8 | Deterministic scorers for market state, structural quality, propagation, exit, and calibration. |
| `lib-validation` | L3 gates, L5 tier | Hard-gate engine, score aggregation, verdict, and deployment tier. |
| `lib-persistence` | cross-cutting | JPA entities, repositories, Flyway migrations, audit/history storage. |
| `lib-backtest` | supports L8 | Deterministic replay harness for historical candidates and bars. |
| `lib-execution` | planned | Design docs for future broker execution. Order placement is not implemented. Schwab OAuth and market-data support currently live in `app`, `lib-marketdata`, and `lib-persistence`. |

Only `app` produces a bootable jar. Libraries are plain jars consumed by `app`.
See [ARCHITECTURE.md](ARCHITECTURE.md) for the MD-layer map.

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

`lib-domain` is the exception: it owns shared domain language under
`dev.reddragon.domain.models`. External API wire DTOs, controller DTOs, and JPA
entities intentionally stay in their owning modules.

## Project Conventions

* Lombok is the default for value objects, service constructors, and loggers.
* No nested classes. Every class, enum, record, and interface lives in its own
  top-level file.
* Shared domain models live in `lib-domain`; shared numeric helpers live in
  `lib-math`.
* `lib-analytics` is deterministic and side-effect free.
* Schwab provider DTOs stay inside `lib-marketdata/services/provider/schwab/`.

## HTTP API Surface

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/` | Static review dashboard. |
| GET | `/health` | Liveness check. |
| GET | `/actuator/metrics` | Micrometer metrics. |
| POST | `/api/pipeline/manual` | Run a manually supplied candidate through the full pipeline. |
| GET | `/api/pipeline/sec/{cik}` | Fetch SEC filings for a CIK and run them through the pipeline. |
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
