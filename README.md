# red-dragon

Probabilistic market-state intelligence platform.

## What this is

A trade-candidate pipeline. Ideas flow in from external sources (SEC EDGAR filings,
manual entry), get enriched with market-data context, scored against the current market
regime and an asymmetry rubric, then filtered by a seven-dimension validation layer.
What survives is presented to the trader for review with a full reasoning chain attached.

The system is candidate-in / verdict-out. It does not track a portfolio,
does not place orders, and does not analyze the trader's history. It
augments discretionary decisions, it does not replace them.

## Pipeline

```
[ ingestion ]  SEC filings, manual candidates
     |
     v
[ enrichment ] market-data features (VWAP, ATR, range position, liquidity, gap)
     |
     v
[ analytics ]  regime classifier + asymmetry scorer (7-dimension disequilibrium model)
     |
     v
[ validation ] hard gates + score aggregation -> PASS / WATCH / REJECT + deployment tier
     |
     v
[ review ]     trader sees survivors with full reasoning chain and risk flags
```

## Module layout

| Module             | MD layers           | Purpose                                                                   |
| ------------------ | ------------------- | ------------------------------------------------------------------------- |
| `app`              | (wires all)         | Only deployable artifact. Spring Boot main, web layer, pipeline wiring.   |
| `lib-ingestion`    | L1, L2 (candidates) | Pull candidate inputs: SEC EDGAR 8-K filings, manual entry.               |
| `lib-marketdata`   | L2 (market data)    | EOD and intraday price/volume retrieval, VWAP/ATR/range-position features.|
| `lib-analytics`    | L3, L4, L5, L6, L8  | Regime classifier + asymmetry scorer. Pure deterministic functions.       |
| `lib-validation`   | L3 (gates), L5      | Hard-rules engine + score aggregation → trade verdict + deployment tier.  |
| `lib-persistence`  | cross-cutting       | JPA entities, repositories, Flyway migrations.                            |
| `lib-backtest`     | supports L8         | Deterministic replay harness for historical candidates and bars.          |
| `lib-execution`    | (planned, post-L5)  | Future Schwab order placement, dry-run gated. Doc-only as of 2026-05-10.  |

Only `app` produces a bootable jar. Libraries are plain jars consumed by `app`.

> **MD-layer reference:** see [ARCHITECTURE.md](ARCHITECTURE.md) for the full
> mapping between the eight-layer trading-framework spec and the Java packages
> in this repo. Every package has a `package-info.java` calling out which MD
> layer it implements.

## Package layout inside each module

Every module follows the same shape:

```
<module>/src/main/java/dev/reddragon/<module>/
    models/      — value objects, DTOs, snapshots, records, enums
    domains/     — JPA entities (lib-persistence only)
    services/    — business logic; sub-packaged where useful (e.g. services/sec/, services/engine/, services/provider/schwab/)
    controllers/ — HTTP controllers (app only)
    utilities/   — pure static helpers
    config/      — Spring @Configuration and @Value-annotated properties classes
```

MD-layer groupings live one level inside `services/` so both axes (type and
layer) are visible in the file tree — e.g.
`lib-analytics/services/structural/`, `services/classification/`, etc.

## Project conventions for contributors

* **Lombok** is the default for value objects (`@Value`), service constructors
  (`@RequiredArgsConstructor`), and loggers (`@Slf4j`). Do not write manual
  builders, getters, or `LoggerFactory.getLogger(...)`.
* **No nested classes.** Every class, enum, record, and interface lives in its
  own top-level file. Cache entries and Jackson DTOs included.
* **Pure functions in `lib-analytics`.** No I/O, no static state, no portfolio
  awareness — each scorer takes a snapshot in and returns a 0.0–1.0 score out.
* **Provider adapters stay sealed.** Schwab DTOs are package-private and never
  leak past `lib-marketdata/services/provider/schwab/`.

## HTTP API surface

| Method | Path                                | Purpose                                              |
| ------ | ----------------------------------- | ---------------------------------------------------- |
| GET    | `/`                                 | Static review dashboard (HTML; calls the APIs below). |
| GET    | `/health`                           | Liveness check.                                      |
| GET    | `/actuator/metrics`                 | Micrometer metric registry (incl. pipeline latency). |
| POST   | `/api/pipeline/manual`              | Run a manually-supplied candidate through the full pipeline. |
| GET    | `/api/pipeline/sec/{cik}`           | Fetch SEC filings for a CIK and run each through the pipeline. |
| GET    | `/api/pipeline/sec/watch-list`      | Run multiple CIKs through the pipeline (also called by the daily scheduler). |
| POST   | `/api/review/manual`                | Ad-hoc manual review without persistence.            |
| GET    | `/api/review/candidates`            | List recent PASS / WATCH verdicts with reasoning.    |
| GET    | `/api/stats/opportunity-quality`    | Conviction-band mix, deployment-tier mix, and top symbols by score. |
| POST   | `/api/backtest`                     | Run a replay against a list of historical frames.    |
| POST   | `/market-structure/intraday`        | Derive intraday structure snapshot from bar list.    |
| POST   | `/api/exit-signal`                  | **L7** — HOLD / TIGHTEN / SCALE_OUT / EXIT_NOW for an open position. |
| POST   | `/api/calibration`                  | Append realized trade outcomes; returns a drift report. |
| GET    | `/api/calibration`                  | Current calibration drift report from all stored outcomes. |
| GET    | `/api/calibration/summary`          | Rolling summary: win rate, avg return, avg drawdown. |
| GET    | `/api/calibration/outcomes`         | Recent outcome rows; supports per-symbol filtering and CSV export. |
| GET    | `/api/schwab/oauth/authorize-url`   | URL the trader visits to grant Schwab API access.    |
| GET   