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
--   * validation_verdict: (candidate_id, created_at) — a candidate can
--     legitimately have multiple verdicts over time (re-evaluation
--     when inputs change), but never two with the same created_at. The
--     V13 `updated_at` column distinguishes mutations; created_at
--     distinguishes new evaluations.
--
--   * backtest_result: (run_id, candidate_id) — a single backtest run
--     produces at most one row per candidate. Reruns get a new run_id.
--
-- SAFETY: if duplicates exist in pre-existing data, the constraint
-- creation will FAIL. Operators must run the duplicate-detection
-- pre-checks below and clean up (typically keeping the highest id per
-- key) before the migration applies cleanly:
--
--   -- validation_verdict duplicates:
--   select candidate_id, created_at, count(*)
--   from validation_verdict
--   group by candidate_id, created_at
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

-- Idempotency key on validation_verdict.
create unique index if not exists uk_validation_verdict_candidate_time
    on validation_verdict(candidate_id, created_at);

-- Idempotency key on backtest_result.
create unique index if not exists uk_backtest_result_run_candidate
    on backtest_result(run_id, candidate_id);
