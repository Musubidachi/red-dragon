-- V13: optimistic locking (@Version) + missing audit columns.
--
-- Backs lib-persistence/REVIEW.md Findings #5 ("no @Version") and #6
-- ("most tables lack created_at / updated_at"). Three column families:
--
--   * version       — `@Version` optimistic-lock column on every entity that
--                     gets mutated after insert (so concurrent writes throw
--                     OptimisticLockException instead of last-write-wins).
--                     Defaults to 0 for existing rows; Hibernate bumps it
--                     on every UPDATE.
--   * created_at    — wall-clock instant the DB first saw the row. Distinct
--                     from semantic timestamps like `observed_at`,
--                     `tested_at`, `imported_at`, or `issued_at`. Lets L8
--                     calibration measure ingestion latency (world → DB).
--   * updated_at    — wall-clock instant the row was last modified.
--                     Application-managed via JPA @PreUpdate; the DB
--                     `default current_timestamp` only seeds the initial
--                     value for backfilled rows.
--
-- Backfill: every existing row gets `current_timestamp` for created_at /
-- updated_at and `0` for version. Application code is expected to honour
-- the version once Hibernate has been redeployed.
--
-- This migration is idempotent at the column-add level: a re-run after a
-- partial failure can be re-applied because every clause uses
-- `if not exists` semantics. (Postgres syntax; matches the project's
-- prior migrations.)

-- ---------------------------------------------------------------
-- Version columns (optimistic locking) on every mutable entity.
-- ---------------------------------------------------------------
alter table candidate           add column if not exists version bigint default 0 not null;
alter table validation_verdict  add column if not exists version bigint default 0 not null;
alter table verdict_override    add column if not exists version bigint default 0 not null;
alter table trader_note         add column if not exists version bigint default 0 not null;
alter table schwab_token        add column if not exists version bigint default 0 not null;

-- ---------------------------------------------------------------
-- created_at on tables that don't have one yet.
-- (market_bar got it in V11; intraday_bar got it in V8;
--  trader_note got it in V5; validation_verdict got it in V1.)
-- ---------------------------------------------------------------
alter table candidate                   add column if not exists created_at timestamp default current_timestamp not null;
alter table analytics_snapshot          add column if not exists created_at timestamp default current_timestamp not null;
alter table market_snapshot             add column if not exists created_at timestamp default current_timestamp not null;
alter table market_quote_observation    add column if not exists created_at timestamp default current_timestamp not null;
alter table verdict_override            add column if not exists created_at timestamp default current_timestamp not null;
alter table schwab_token                add column if not exists created_at timestamp default current_timestamp not null;
alter table backtest_result             add column if not exists created_at timestamp default current_timestamp not null;
alter table calibration_outcome         add column if not exists created_at timestamp default current_timestamp not null;
alter table trade_history_import_batch  add column if not exists created_at timestamp default current_timestamp not null;
alter table trade_history_record        add column if not exists created_at timestamp default current_timestamp not null;

-- ---------------------------------------------------------------
-- updated_at on mutable entities only. Application code populates
-- this via JPA @PreUpdate; the default seeds the initial value so
-- existing rows aren't NULL after the column add.
-- ---------------------------------------------------------------
alter table candidate           add column if not exists updated_at timestamp default current_timestamp not null;
alter table validation_verdict  add column if not exists updated_at timestamp default current_timestamp not null;
alter table verdict_override    add column if not exists updated_at timestamp default current_timestamp not null;
alter table trader_note         add column if not exists updated_at timestamp default current_timestamp not null;
alter table schwab_token        add column if not exists updated_at timestamp default current_timestamp not null;

-- ---------------------------------------------------------------
-- Useful indexes on created_at — many controllers and the L8
-- calibration loop filter by "rows added since X". Without an index
-- those queries are sequential scans.
-- ---------------------------------------------------------------
create index if not exists idx_candidate_created_at         on candidate(created_at);
create index if not exists idx_analytics_snapshot_created_at on analytics_snapshot(created_at);
create index if not exists idx_market_snapshot_created_at   on market_snapshot(created_at);
create index if not exists idx_backtest_result_created_at   on backtest_result(created_at);
create index if not exists idx_calibration_outcome_created_at on calibration_outcome(created_at);
