# lib-validation

> **MD layer:** owns the hard-gate half of **L3 (Structural Validation)** and
> the tier decision in **L5 (Deployment)**. See [ARCHITECTURE.md](../ARCHITECTURE.md)
> for the full mapping.

`lib-validation` is responsible for turning candidate context and analytical signals into an actionable review verdict.

This module should answer one question:

> Should this candidate be shown to the trader as PASS, WATCH, or REJECT?

Validation is the gatekeeper. It should be stricter than analytics because its job is to prevent low-quality or incomplete candidates from cluttering the review surface.

## Responsibilities

- Apply hard rejection rules.
- Aggregate analytics outputs into a final candidate verdict.
- Produce human-readable rejection, watch, and pass reasons.
- Separate required-data checks from trade-quality checks.
- Keep verdict logic deterministic and auditable.

Possible verdicts:

- `PASS` — candidate is strong enough for trader review.
- `WATCH` — candidate has promise but is missing confirmation or has moderate issues.
- `REJECT` — candidate fails one or more hard rules.

## Non-responsibilities

This library should not:

- Pull raw candidates from external sources.
- Fetch market data directly from providers.
- Perform low-level indicator calculations.
- Persist verdicts directly unless routed through persistence contracts.
- Place orders or size positions.
- Replace trader judgment.

## Expected flow

```text
candidate + market-data snapshot + analytics snapshot
    -> required data checks
    -> hard rules
    -> score aggregation
    -> PASS / WATCH / REJECT verdict
    -> review surface / persistence
```

## Rule categories

### Required-data rules

These rules determine whether the system has enough reliable information to judge the candidate.

Examples:

- ticker is missing
- market data is stale
- insufficient price history
- no source provenance
- malformed candidate metadata

### Hard rejection rules

These rules reject candidates that should not be reviewed unless explicitly overridden later.

Examples:

- liquidity below minimum threshold
- volatility above allowed risk threshold
- spread or range too wide
- known data-quality issue
- unsupported security type

### Watch rules

These rules identify candidates that are not bad enough to reject but not clean enough to pass.

Examples:

- catalyst exists but price confirmation is weak
- setup is promising but market regime is hostile
- liquidity is acceptable but thin
- asymmetry score is near the pass threshold

### Pass rules

A candidate should pass only when required data is present, hard rules are clear, and the aggregate score meets the configured threshold.

## Design guidance

### Validate before scoring

Do not aggregate scores when required data is missing. A candidate with incomplete data should be rejected or watched with a clear reason, not given a misleading numerical verdict.

### Return complete reasoning

The output should explain the verdict using reason codes and display-ready text.

Example reason codes:

- `MISSING_MARKET_DATA`
- `INSUFFICIENT_HISTORY`
- `LIQUIDITY_BELOW_MINIMUM`
- `ASYMMETRY_SCORE_STRONG`
- `REGIME_NOT_SUPPORTIVE`
- `PRICE_CONFIRMATION_WEAK`

### Keep thresholds explicit

Thresholds should be visible and configurable. Avoid burying magic numbers inside validator methods.

## Suggested package layout

```text
lib-validation
└── src/main/java/dev/reddragon/validation
    ├── verdict         # verdict models and reason codes
    ├── rule            # hard rules and required-data checks
    ├── aggregate       # score aggregation and verdict selection
    └── config          # thresholds and validation settings
```

## First implementation target

1. Define verdict, reason-code, and validation-result models.
2. Implement required-data checks.
3. Implement hard liquidity and data-quality rules.
4. Aggregate analytics score into PASS / WATCH / REJECT.
5. Return a full reasoning chain for display in the review UI.

## Testing expectations

Tests should cover:

- missing required data
- hard rejection behavior
- watch threshold boundaries
- pass threshold boundaries
- reason-code ordering
- deterministic verdicts for fixed candidate snapshots
- score aggregation math

Validation tests should read like business-rule documentation. Each test name should make the rule obvious.
