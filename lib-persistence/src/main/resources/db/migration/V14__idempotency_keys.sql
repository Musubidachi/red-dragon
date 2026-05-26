-- V14: idempotency keys on validation_verdict and backtest_result.
--
-- Closes lib-persistence/REVIEW.md Finding #11 ("no semantic uniqueness
-- key; duplicate writes are possible"). The synthetic `id` columns alone
-- don't protect against a retried POST /api/pipeline/manual or a
-- duplicate backtest run silently inserting parallel rows; the L8
-- calibration loop counts these as independent samples and skews
-- rolling win-rate metrics.
--
-- Natural keys chosen:
--
--   * validation_verdict: idempotency_key. The application supplies a
--     deterministic SHA-256 fingerprint of the semantic validation output
--     (candidate id, symbol, verdict, deployment tier, score, factors,
--     reason codes, and explanations). Retried writes of the same verdict
--     collide even though created_at is generated at write time; genuine
--     re-evaluations with changed inputs/results get a different key.
--     Historical rows from before this column existed remain NULL because
--     the exact output fingerprint was not persisted; PostgreSQL unique
--     indexes allow multiple NULLs, so this does not block migration.
--
--   * backtest_result: (run_id, candidate_id) - a single backtest run
--     produces at most one row per candidate. Reruns get a new run_id.
--
-- SAFETY: if duplicates exist in pre-existing data, the constraint
-- creation can FAIL. Operators must run the duplicate-detection
-- pre-checks below and clean up (typically keeping the highest id per
-- key) before the migration applies cleanly:
--
--   -- validation_verdict duplicates after the application starts writing
--   -- idempotency keys:
--   select idempotency_key, count(*)
--   from validation_verdict
--   where idempotency_key is not null
--   group by idempotency_key
--   having count(*) > 1;
--
--   -- backtest_result duplicates:
--   select run_id, candidate_id, count(*)
--   from backtest_result
--   group by run_id, candidate_id
--   having count(*) > 1;
--
-- Re-runnable: every clause uses `if not exists` so a partial-failure
-- recovery just continues from where the previous attempt stopped.

-- Idempotency key on validation_verdict. Nullable for historical rows.
alter table validation_verdict
    add column if not exists idempotency_key varchar(128);

create unique index if not exists uk_validation_verdict_idempotency_key
    on validation_verdict(idempotency_key);

-- Idempotency key on backtest_result.
create unique index if not exists uk_backtest_result_run_candidate
    on backtest_result(run_id, candidate_id);
