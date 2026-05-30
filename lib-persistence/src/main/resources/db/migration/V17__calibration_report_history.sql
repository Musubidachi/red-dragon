-- V17: persist calibration report outputs so POST /api/calibration has a
-- durable report history instead of only returning the computed response.

create table calibration_report (
    id bigserial primary key,
    generated_at timestamp with time zone not null,
    source varchar(64) not null,
    drift_level varchar(32) not null,
    historical_win_rate double precision not null,
    average_return double precision not null,
    average_drawdown double precision not null,
    findings text,
    recommendations text,
    created_at timestamp with time zone not null default current_timestamp
);

create index idx_calibration_report_generated_at
    on calibration_report (generated_at desc);
