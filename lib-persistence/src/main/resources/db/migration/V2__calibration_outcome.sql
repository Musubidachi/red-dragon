create table if not exists calibration_outcome (
    id bigserial primary key,
    candidate_id varchar(128) not null,
    symbol varchar(16) not null,
    observed_at timestamp not null,
    structural_reality_score double precision not null,
    material_significance_score double precision not null,
    earlyness_score double precision not null,
    equilibrium_quality_score double precision not null,
    reflexivity_potential_score double precision not null,
    asymmetry_score double precision not null,
    regime_compatibility_score double precision not null,
    deployment_confidence_score double precision not null,
    realized_return double precision not null,
    max_drawdown double precision not null,
    days_held integer not null,
    thesis_worked boolean not null
);
