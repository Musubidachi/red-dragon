alter table market_snapshot
    add column relative_volume double precision;

alter table market_snapshot
    add column vwap_deviation double precision;

alter table market_snapshot
    add column directional_persistence double precision;
