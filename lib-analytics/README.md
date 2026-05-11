# lib-analytics

`lib-analytics` is responsible for turning enriched candidate context into explainable analytical signals.

This module should answer two questions:

> What market state is this candidate operating in?
>
> Does the setup appear asymmetric enough to deserve attention?

The analytics layer should remain deterministic first. Probabilistic or machine-learning methods can be added later, but the early version should be transparent enough that a trader can challenge every factor.

## Responsibilities

- Classify the current market or symbol-level regime.
- Score candidate asymmetry using explainable factors.
- Produce reason codes and factor-level contributions.
- Keep scoring functions pure where practical.
- Avoid hidden side effects, persistence, or external data retrieval.

Possible analytical outputs:

- regime label
- regime confidence
- asymmetry score
- upside / downside factor notes
- liquidity risk note
- volatility context
- reason codes used by validation

## Non-responsibilities

This library should not:

- Pull candidates from external sources.
- Fetch market bars directly from providers.
- Persist analytics snapshots directly unless routed through persistence contracts.
- Produce final PASS / WATCH / REJECT verdicts by itself.
- Place trades or manage portfolio exposure.

## Expected flow

```text
candidate + market-data snapshot
    -> regime classifier
    -> asymmetry scorer
    -> analytical snapshot with reason codes
    -> validation
```

## Design guidance

### Make the first version rule-based

Start with simple rules before probabilistic scoring. A transparent rule-based baseline gives you something to compare against later.

Example regime inputs:

- broad index trend
- volatility level
- candidate volatility vs normal range
- volume / liquidity profile
- gap behavior
- price location inside recent range

Example asymmetry inputs:

- distance to invalidation
- potential catalyst strength
- liquidity quality
- volatility expansion or compression
- current range position
- market regime alignment

### Return explanations, not just numbers

Every score should include reason codes or factor contributions. A score without an explanation is not useful for discretionary review.

Example reason codes:

- `RANGE_POSITION_FAVORABLE`
- `LIQUIDITY_ACCEPTABLE`
- `ATR_TOO_HIGH`
- `REGIME_RISK_ON`
- `CATALYST_PRESENT`
- `DOWNSIDE_TOO_WIDE`

### Keep functions testable

Given the same candidate and market-data snapshot, analytics should produce the same result every time. Avoid network access, time-dependent behavior, and mutable global state inside scoring functions.

## Suggested package layout

```text
lib-analytics
└── src/main/java/dev/reddragon/analytics
    ├── regime          # regime classifier and regime models
    ├── asymmetry       # asymmetry scorer and factor contributions
    ├── signal          # analytical signal contracts and reason codes
    └── config          # scoring weights and thresholds
```

## First implementation target

1. Define regime labels and analytical snapshot models.
2. Implement a deterministic regime classifier.
3. Implement a simple weighted asymmetry scorer.
4. Return factor-level contributions and reason codes.
5. Keep all thresholds configurable from a plain object or properties mapping.

## Testing expectations

Tests should cover:

- deterministic scoring for fixed inputs
- boundary behavior around thresholds
- missing or incomplete market-data snapshots
- reason-code generation
- score aggregation math
- regime classification for known scenarios

Do not test analytics by depending on live market data. Feed fixed snapshots into the scoring functions.
