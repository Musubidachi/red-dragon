# lib-backtest Review

Last updated: 2026-05-27

This review summarizes the backtest module after the local cleanup pass. The
highest-priority production/backtest drift risk at the validation-input
boundary is now guarded by the shared validation-input factory.

## Current Open Work

No open module-local issues are currently tracked.

## Implemented Surface

Implemented today:

* `BacktestFrame` for candidate plus historical bars.
* `BacktestOutcome` for full pipeline output per frame.
* `BacktestMetrics` for frame count, average score, and verdict counts.
* `BacktestReport` for named strategy results and summary text.
* Stateless `BacktestReplayEngine` that runs marketdata -> analytics ->
  validation without persistence or live-data fetching.
* Historical-style calibration fixtures that drive deterministic OHLCV bars
  through marketdata, analytics, validation, backtest, and calibration seams.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| Service style | `BacktestReplayEngine` uses Lombok constructor style. |
| Locale stability | `BacktestReport.summary()` uses `Locale.ROOT`. |
| Immutability | `BacktestMetrics` wraps verdict counts defensively. |
| Null guards | `BacktestOutcome`, `BacktestReport`, and strategy-name inputs reject nulls. |
| Package docs | `package-info.java` files were added. |
| Dead branches | Summary/pass-rate logic assumes non-null metrics after constructor validation. |
| Shared validation input | `BacktestReplayEngine` and the production orchestrator both use `CandidateValidationInputFactory`; backtest tests assert replay validation consumes the injected factory output. |
| Determinism and metrics | Backtest tests now assert repeated-run equality, manual direct-service equivalence, verdict distribution, and average score aggregation. |
| Historical calibration fixture | `HistoricalMarketDataCalibrationFixtureTest` pins Wilder ATR, session VWAP, realized volatility, validation output, backtest replay, and calibration drift behavior against deterministic offline bars. |
| App wiring verification | `BacktestControllerWiringTest` verifies the app `BacktestReplayEngine` uses the configured pipeline beans and that the controller persists a smoke replay through candidate, backtest-result, and calibration repositories. |

## Non-Responsibilities

Backtest does not fetch live SEC or market data, persist results directly,
simulate fills/slippage, model portfolio compounding, or produce forward-looking
performance promises. The `app` layer owns persistence and HTTP surfaces.

Root tracking: [../ISSUES.md](../ISSUES.md).
