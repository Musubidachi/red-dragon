-- V16: widen narrative/import text fields that are not queried by prefix or
-- equality. These values can exceed the old varchar budgets during real-world
-- ingestion, review, analytics explanation, and import-warning flows.

alter table candidate
    alter column summary type text;

alter table market_snapshot
    alter column notes type text;

alter table analytics_snapshot
    alter column reason_notes type text;

alter table validation_verdict
    alter column reason_codes type text;

alter table validation_verdict
    alter column explanations type text;

alter table validation_verdict_reason
    alter column explanation type text;

alter table trader_note
    alter column note_text type text;

alter table verdict_override
    alter column reason type text;

alter table market_quote_observation
    alter column notes type text;

alter table trade_history_import_batch
    alter column warnings type text;
