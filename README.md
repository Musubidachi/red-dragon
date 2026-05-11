# red-dragon

Probabilistic market-state intelligence platform.

## What this is

A trade-candidate pipeline. AI-surfaced ideas flow in from external sources,
get enriched with market-data context, scored against the current market
regime and an asymmetry rubric, then filtered by a validation layer. What
survives is presented to the trader for review.

The system is candidate-in / verdict-out. It does not track a portfolio,
does not place orders, and does not analyze the trader's history. It
augments discretionary decisions, it does not replace them.

## Pipeline

```
[ ingestion ]  SEC filings, news/RSS, scanners, macro feeds
     |
     v
[ enrichment ] market-data features (VWAP, ATR, range position, liquidity)
     |
     v
[ analytics ]  regime classifier + asymmetry scorer
     |
     v
[ validation ] hard rules + score aggregation -> PASS / WATCH / REJECT
     |
     v
[ review ]     trader sees survivors with the reasoning chain attached
```

## Module layout

| Module             | Purpose                                                     |
| ------------------ | ----------------------------------------------------------- |
| `app`              | Only deployable artifact. Spring Boot main + web layer.     |
| `lib-ingestion`    | Pull candidate inputs: SEC filings, news/RSS, scanners, macro feeds. |
| `lib-marketdata`   | EOD price/volume retrieval, VWAP/ATR/range-position features. |
| `lib-analytics`    | Regime classifier + asymmetry scorer. Pure functions.       |
| `lib-validation`   | Hard rules engine + score aggregation -> trade verdict.     |
| `lib-persistence`  | JPA entities, repositories, Flyway migrations.              |

Only `app` produces a bootable jar. Libraries are plain jars consumed by `app`.

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

## Roadmap (sequenced)

1. Persistence schema (`candidate`, `enrichment_snapshot`, `regime_snapshot`, `validation_verdict`, `market_bar`) + Flyway migrations.
2. One ingestion source end-to-end. Recommend SEC EDGAR (deterministic, free, structured).
3. EOD market-data provider + a small feature set (VWAP-proxy, ATR, range position).
4. Rule-based regime classifier (deterministic before probabilistic).
5. Hard-rules engine + first asymmetry scorer (simple weighted sum, transparent factors).
6. Read-only HTML review surface that lists today's PASS / WATCH candidates with reasoning.

Things explicitly deferred: portfolio tracking, order execution, ML-based
narrative scoring (FinBERT etc.), Kafka/Redis, Angular frontend, historical
trade analysis. Each can earn its way in once the core pipeline is producing
verdicts the trader trusts.

## Status

Skeleton only. No business logic yet.
