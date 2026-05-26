-- V5: Add relative_volume, vwap_deviation, and directional_persistence to market_snapshot.
-- All columns are nullable to allow backward-compatible reads of rows created before this migration.

alter table market_snapshot
    add column if not exists relative_volume          double precision,
    add column if not exists vwap_deviation           double precision,
    add column if not exists directional_persistence  double precision;
