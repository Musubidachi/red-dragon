# lib-backtest

> **MD layer:** supports **L8 (Meta-System Adaptation)** by replaying historical
> frames through the same pipeline as production, so calibration analysis has
> something to chew on. See [ARCHITECTURE.md](../ARCHITECTURE.md) for the full mapping.

`lib-backtest` is a deterministic replay harness for testing the full candidate pipeline against historical data.

This module should answer one question:

> If I had run this candidate through the pipeline on this date with these bars, what verdict would I have received?

Backtesting does not predict future results. It validates that the pipeline behaves consistently, surfaces threshold sensitivity, and gives the trader a way to calibrate trust in the validation model before risking capital.

## Responsibilities

- Accept historical candidates paired with their historical market bars.
- Feed each pair through the same enrichment → analytics → validation pipeline used in production.
- Accumulate per-frame outcomes into a report with summary metrics.
- Keep replay logic deterministic: identical inputs must produce identical outputs.

## Non-responsibilities

This library should not:

- Fetch live market data or SEC filings.
- Persist results directly (the `app` layer owns that responsibility).
- Simulate fills, slippage, or portfolio compounding.
- Produce forward-looking performance estimates.
- Replace real trading discipline with statistics.

## Data flow

```text
List<BacktestFrame>  (candidate + historical bars)
    -> MarketFeatureCalculator.process(symbol, bars)
    -> DeterministicAnalyticsService.process(candidate, marketData)
    -> DisequilibriumValidationEngine.process(validationInput)
    -> BacktestOutcome  (candidate + marketData + analytics + validation)
    -> BacktestReport   (strategyName + BacktestMetrics + List<BacktestOutcome>)
```

## Key types

| Type                  | Description                                              |
| --------------------- | -------------------------------------------------------- |
| `BacktestFrame`       | One candidate paired with the bars available on that date. |
| `BacktestOutcome`     | Full pipeline output for one frame.                      |
| `BacktestMetrics`     | Summary stats: total frames, average score, verdict distribution. |
| `BacktestReport`      | Named strategy result containing metrics and all outcomes. |
| `BacktestReplayEngine`| Stateless service that maps frames to outcomes.          |

## HTTP Endpoints

* `POST /api/backtest` - accepts a JSON body with `strategyName` and a list of
  frames, returns a full `BacktestReport`, and stores per-frame results.
* `GET /api/backtest/results/{runId}` - returns stored results for a run.
* `GET /api/backtest/runs` - lists known run ids.

See `BacktestController` in `app` for request shape.

## Design guidance

### Keep it stateless

`BacktestReplayEngine` holds no mutable state. Each call to `process()` is independent.
This makes concurrent or batch replay straightforward.

### Use the same engine as production

The backtest intentionally uses `MarketFeatureCalculator`, `DeterministicAnalyticsService`,
and `DisequilibriumValidationEngine` without any special-casing. If the production engine
changes, backtest behavior changes with it — that is a feature, not a bug.

### Separate fixture loading from replay logic

Tests should build `BacktestFrame` fixtures inline or from fixture files. The replay engine
should not know how frames are sourced.

## Testing expectations

Tests should cover:

- empty frame list produces an empty report with zero metrics
- single frame: outcome matches a direct engine call with the same inputs
- verdict distribution counts are accurate
- average score matches manual calculation
- deterministic: same frames in same order produce the same report every time
