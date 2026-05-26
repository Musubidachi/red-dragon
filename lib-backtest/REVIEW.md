# lib-backtest Review

Last updated: 2026-05-26

This review summarizes the backtest module after the local cleanup pass. The
main remaining risk is production/backtest drift at the validation-input
boundary.

## Current Open Work

| Priority | Issue | Status | Impact | Next action |
| --- | --- | --- | --- | --- |
| High | Shared validation-input factory | Open | `BacktestReplayEngine` and the production orchestrator duplicate `CandidateValidationInput` construction and risk-threshold literals. | Extract a shared factory in `lib-validation` or `lib-domain` and make both callers use it. |
| Medium | Determinism and metric tests | Open | README promises deterministic replay and metric correctness, but tests do not fully pin those guarantees. | Add direct-engine equivalence, verdict-distribution, average-score, and repeated-run equality tests. |
| Medium | Builder migration for validation input | Open | The engine still relies on positional `CandidateValidationInput` construction. | Use the builder added in `lib-domain` or a named shared factory. |
| Low | App wiring verification | Open | Local module construction is fixed, but the app consumption path should be checked. | Verify `BacktestReplayEngine` bean creation and controller wiring in `app`. |

## Implemented Surface

Implemented today:

* `BacktestFrame` for candidate plus historical bars.
* `BacktestOutcome` for full pipeline output per frame.
* `BacktestMetrics` for frame count, average score, and verdict counts.
* `BacktestReport` for named strategy results and summary text.
* Stateless `BacktestReplayEngine` that runs marketdata -> analytics ->
  validation without persistence or live-data fetching.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| Service style | `BacktestReplayEngine` uses Lombok constructor style. |
| Locale stability | `BacktestReport.summary()` uses `Locale.ROOT`. |
| Immutability | `BacktestMetrics` wraps verdict counts defensively. |
| Null guards | `BacktestOutcome`, `BacktestReport`, and strategy-name inputs reject nulls. |
| Package docs | `package-info.java` files were added. |
| Dead branches | Summary/pass-rate logic assumes non-null metrics after constructor validation. |

## Non-Responsibilities

Backtest does not fetch live SEC or market data, persist results directly,
simulate fills/slippage, model portfolio compounding, or produce forward-looking
performance promises. The `app` layer owns persistence and HTTP surfaces.

Root tracking: [../ISSUES.md](../ISSUES.md).
