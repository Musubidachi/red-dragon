# lib-persistence Review

Last updated: 2026-05-26

This review summarizes the persistence module after the V10-V14 migration and
mapper fix passes. Runtime Flyway files under `src/main/resources/db/migration`
remain the schema source of truth.

## Current Open Work

| Priority | Issue | Status | Impact | Next action |
| --- | --- | --- | --- | --- |
| Medium | Repository/migration integration tests | Open | Entity mapping, Flyway migration validity, relationships, and constraints are only lightly covered. | Add `@DataJpaTest` or equivalent migration-backed integration tests for major entities. |
| Medium | Truncation-prone text columns | Open | Long notes, explanations, quote notes, or import warnings can exceed fixed varchar limits. | Move unbounded text to `TEXT`/`CLOB` or normalize structures where queryability matters. |
| Low | Generated-id positional constructors | Open | Remaining generated-id entities still expose `id` through all-args construction. | Finish builder migration or add construction factories that omit generated IDs. |

## Implemented Surface

Implemented today:

* JPA entities and repositories for candidates, market bars, intraday bars,
  market snapshots, quote observations, analytics snapshots, validation
  verdicts, normalized verdict reasons, overrides, trader notes, backtest
  results, calibration outcomes, Schwab tokens, and trade-history imports.
* `PersistenceMapper` for converting pipeline domain objects to persistence
  entities.
* Flyway migrations through V14.
* Optimistic locking and audit timestamps on mutable tables.
* Validation-verdict idempotency key based on an application-supplied
  deterministic fingerprint.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| README/schema drift | README lists current tables and migrations; stale schema drafts moved under `db/drafts`. |
| Foreign keys | Backtest, market snapshot, and validation verdict candidate relationships were retrofitted with FKs/indexes. |
| Validation reasons | `validation_verdict_reason` normalizes reason codes and explanations into a child table. |
| Optimistic locking | Mutable entities gained `@Version` columns. |
| Audit timestamps | Mutable and arrival-tracked tables gained created/updated timestamps where appropriate. |
| High-traffic builders | Main constructed entities use builders in mapper/controller paths. |
| Market bar audit | `market_bar.created_at` was added to match newer intraday-bar audit behavior. |
| Idempotency | Validation verdicts and backtest results gained uniqueness protection; V14 uses deterministic fingerprints. |
| SQL correctness | V12 recursive split migration is PostgreSQL-correct with `WITH RECURSIVE`. |
| Schwab token storage | Access/refresh token fields now use AES-GCM encryption via a JPA converter. Set `red-dragon.persistence.schwab-token-encryption-key` or `RED_DRAGON_PERSISTENCE_SCHWAB_TOKEN_ENCRYPTION_KEY` to `base64:<32-byte AES key>`. Legacy plaintext rows remain readable; new or updated rows are encrypted, and old audit rows must be re-saved or purged for retroactive cleanup. |

## Design-Only Work

[TRADE_DECISION.md](TRADE_DECISION.md) remains design-only. No broker order,
fill, account, position, or trade-decision tables exist yet.

Root tracking: [../ISSUES.md](../ISSUES.md).
