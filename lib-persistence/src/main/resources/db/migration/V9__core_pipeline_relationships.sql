alter table market_snapshot
    add constraint fk_market_snapshot_candidate
    foreign key (candidate_id) references candidate(candidate_id);

alter table validation_verdict
    add constraint fk_validation_verdict_candidate
    foreign key (candidate_id) references candidate(candidate_id);

create index idx_market_snapshot_candidate_id
    on market_snapshot(candidate_id);

create index idx_market_snapshot_symbol_observed_at
    on market_snapshot(symbol, observed_at desc);

create index idx_validation_verdict_candidate_id
    on validation_verdict(candidate_id);
