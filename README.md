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

| Module             | Purpose                                                                   |
| ------------------ | ------------------------------------------------------------------------- |
| `app`              | Only deployable artifact. Spring Boot main, web layer, pipeline wiring.   |
| `lib-ingestion`    | Pull candidate inputs: SEC EDGAR 8-K filings, manual entry.               |
| `lib-marketdata`   | EOD and intraday price/volume retrieval, VWAP/ATR/range-position features.|
| `lib-analytics`    | Regime classifier + asymmetry scorer. Pure deterministic functions.       |
| `lib-validation`   | Seven-dimension hard-rules engine + score aggregation → trade verdict.    |
| `lib-persistence`  | JPA entities, repositories, Flyway migrations.                            |
| `lib-backtest`     | Deterministic replay harness for historical candidates and bars.          |

Only `app` produces a bootable jar. Libraries are plain jars consumed by `app`.

## HTTP API surface

| Method | Path                              | Purpose                                              |
| ------ | --------------------------------- | ---------------------------------------------------- |
| GET    | `/health`                         | Liveness check.                                      |
| POST   | `/api/pipeline/manual`            | Run a manually-supplied candidate through the full pipeline. |
| GET    | `/api/pipeline/sec/{cik}`         | Fetch SEC filings for a CIK and run each through the pipeline. |
| POST   | `/api/review/manual`              | Ad-hoc manual review without persistence.            |
| GET    | `/api/review/candidates`          | List today's PASS / WATCH verdicts with reasoning.   |
| GET    | `/api/stats/opportunity-quality`  | Conviction-band mix, deployment-tier mix, and top symbols by score. |
| POST   | `/api/backtest`                   | Run a replay against a list of historical frames.    |
| POST   | `/market-structure/intraday`      | Derive intraday structure snapshot from bar list.    |

## Build

Requires JDK 21 and Maven 3.9+.

```
mvn -q -DskipTests package
```

Run the app:

```
mvn -pl app spring-boot:run
```

Smoke-check:

```
curl -s http://localhost:8080/health
```

## Status

Core pipeline is operational end-to-end:

- SEC EDGAR 8-K ingestion is live (CIK → candidate)
- Market feature calculation is live (ATR, VWAP proxy, range position, liquidity, gap)
- Regime classification and asymmetry scoring are live (seven-dimension disequilibrium model)
- Validation engine is live (hard gates + score aggregation → PASS / WATCH / REJECT)
- Persistence is live (candidate, verdict, market bar saved on every pipeline run)
- Backtest replay engine is live
- Candidate review surface is live (GET /api/review/candidates)

Things explicitly deferred: portfolio tracking, order execution, ML-based
narrative scoring (FinBERT etc.), Kafka/Redis, Angular frontend, historical
trade analysis. Each can earn its way in once the core pipeline is producing
verdicts the trader trusts.
