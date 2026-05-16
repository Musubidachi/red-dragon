# lib-persistence

> **MD layer:** cross-cutting. Stores durable artifacts produced by ingestion,
> enrichment, analytics, validation, review, backtest, calibration, and Schwab
> OAuth support. See [ARCHITECTURE.md](../ARCHITECTURE.md).

`lib-persistence` owns Red Dragon's database entities, repositories, mappers,
and Flyway migrations.

## Responsibilities

* Define JPA entities and repositories.
* Own Flyway migrations.
* Store candidates, market bars, market snapshots, analytics snapshots,
  validation verdicts, overrides, trader notes, backtest results, calibration
  outcomes, and Schwab OAuth tokens.
* Preserve source provenance and reasoning chains.
* Keep schema changes versioned and reviewable.

## Current Tables

Implemented migration tables:

* `candidate`
* `market_bar`
* `market_snapshot`
* `analytics_snapshot`
* `validation_verdict`
* `verdict_override`
* `trader_note`
* `backtest_result`
* `calibration_outcome`
* `schwab_token`

There are also schema reference files under `db/schema/`. Treat the Flyway files
under `db/migration/` as the runtime source of truth.

## Non-Responsibilities

This library does not fetch external data, score candidates, decide verdicts,
host web controllers, place orders, or contain the Spring Boot main class.

## Current Package Layout

```text
lib-persistence/src/main/java/dev/reddragon/persistence
    domains/      JPA entities
    services/     PersistenceMapper
    services/repositories/ Spring Data repositories
    utilities/    persistence helpers

lib-persistence/src/main/resources/db/migration
    V1__candidate_pipeline_schema.sql
    V2__calibration_outcome.sql
    V3__schwab_token.sql
```

## Still Planned

The trade-decision/order/fill schema in [TRADE_DECISION.md](TRADE_DECISION.md)
is still design-only. No broker order, fill, account, or trade-decision tables
exist yet.

## Testing Expectations

Tests should cover entity mapping, repository behavior, migration validity,
required fields and constraints, relationship integrity, and storage of reason
codes and source metadata.
