-- Backport created_at audit column to market_bar to match the V8 intraday_bar
-- pattern. The V1 schema for market_bar omitted this column; this migration
-- closes the audit-pattern inconsistency tracked in lib-persistence REVIEW.md
-- Finding #10.
--
-- Existing rows get current_timestamp at apply-time (because that's the only
-- timestamp we have for them — the original ingestion clock was not preserved).
-- New rows pick up the default automatically.

alter table market_bar
    add column created_at timestamp default current_timestamp not null;
