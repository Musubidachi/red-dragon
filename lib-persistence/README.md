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
  validation verdicts and reasons, overrides, trader notes, backtest results,
  calibration outcomes, trade-history imports, and Schwab OAuth tokens.
* Preserve source provenance and reasoning chains.
* Keep schema changes versioned and reviewable.

## Current Tables

Implemented migration tables:

* `candidate`
* `market_bar`
* `intraday_bar`
* `market_snapshot`
* `market_quote_observation`
* `analytics_snapshot`
* `validation_verdict`
* `verdict_override`
* `trader_note`
* `backtest_result`
* `calibration_outcome`
* `schwab_token`
* `trade_history_import_batch`
* `trade_history_record`
* `validation_verdict_reason` (V12; normalized child of `validation_verdict`)

There are also pre-migration design drafts under `db/drafts/`. Their version
numbers intentionally do not align with `db/migration/`. Treat the Flyway files
under `db/migration/` as the runtime source of truth.

## Non-Responsibilities

This library does not fetch external data, score candidates, decide verdicts,
host web controllers, place orders, or contain the Spring Boot main class.

## Current Package Layout

```text
lib-persistence/src/main/java/dev/reddragon/persistence
    domains/                JPA entities (15 entities, one file each)
    services/               PersistenceMapper
    services/repositories/  Spring Data repositories (one per entity)
    utilities/              PersistenceStringUtils

lib-persistence/src/main/resources/db/migration   (runtime source of truth)
    V1__candidate_pipeline_schema.sql              candidate, market_bar, market_snapshot, validation_verdict, backtest_result
    V2__calibration_outcome.sql                    calibration_outcome (L8)
    V3__schwab_token.sql                           schwab_token (OAuth state)
    V4__analytics_snapshot.sql                     analytics_snapshot (with FK to candidate)
    V5__trader_notes.sql                           trader_note
    V6__verdict_overrides.sql                      verdict_override
    V7__market_snapshot_extended_fields.sql        adds relative_volume, vwap_deviation, directional_persistence
    V8__market_data_observations_and_trade_history.sql
                                                   intraday_bar, market_quote_observation,
                                                   trade_history_import_batch, trade_history_record
    V9__core_pipeline_relationships.sql            retroactive FKs + indexes on market_snapshot,
                                                   validation_verdict
    V10__backtest_result_candidate_fk.sql          retroactive FK on backtest_result(candidate_id)
    V11__market_bar_audit_column.sql               adds created_at to market_bar (matches V8 intraday_bar)
    V12__validation_verdict_reason.sql             normalizes reason_codes blob into a child table
    V13__version_and_audit_columns.sql             optimistic locking and audit columns
    V14__idempotency_keys.sql                      deterministic validation idempotency and backtest uniqueness

lib-persistence/src/main/resources/db/drafts      pre-migration design notes; NOT applied at runtime
```

## Still Planned

The trade-decision/order/fill schema in [TRADE_DECISION.md](TRADE_DECISION.md)
is still design-only. No broker order, fill, account, or trade-decision tables
exist yet.

## Testing Expectations

Tests should cover entity mapping, repository behavior, migration validity,
required fields and constraints, relationship integrity, and storage of reason
codes and source metadata.
