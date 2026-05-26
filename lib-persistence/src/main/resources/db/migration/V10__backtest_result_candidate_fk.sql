-- Retroactive foreign-key + index for backtest_result(candidate_id).
--
-- backtest_result.candidate_id has existed since V1 but never had an FK to
-- candidate(candidate_id). Orphan rows could be inserted by mistakes in the
-- backtest engine or by replaying against deleted candidate fixtures.
--
-- SAFETY: before applying in production, run
--     SELECT count(*) FROM backtest_result
--      WHERE candidate_id NOT IN (SELECT candidate_id FROM candidate);
-- and delete or repair any orphan rows. This migration will fail if orphans
-- exist.

alter table backtest_result
    add constraint fk_backtest_result_candidate
    foreign key (candidate_id) references candidate(candidate_id);

create index idx_backtest_result_candidate_id
    on backtest_result(candidate_id);
