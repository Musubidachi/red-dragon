# lib-marketdata Review

Last updated: 2026-05-26

This review summarizes the current market-data module after the feature-math
and provider hardening passes. Fixed audit detail is compressed below the open
work.

## Current Open Work

| Priority | Issue | Status | Impact | Next action |
| --- | --- | --- | --- | --- |
| High | Schwab retry/backoff hardening | Open | Current Schwab retry behavior is not yet HTTP-status aware and can retry errors that should fail immediately. | Retry only network failures, 429, and 5xx; propagate non-429 4xx and programmer errors; use jittered exponential backoff. |
| High | VWAP session boundary | Open | `VwapCalculator.process(List<IntradayBar>)` can produce a multi-session VWAP if callers pass multiple days of bars. | Add an explicit session/date API or split cumulative and session VWAP methods. |
| Medium | Remaining provider and builder coverage | Open | Schwab coverage improved, but Yahoo provider and some snapshot builders remain weakly covered. | Add provider mapping/failure tests and snapshot-builder edge cases. |
| Medium | Calibration validation after math changes | Open | Wilder ATR and realized-volatility corrections can shift downstream thresholds. | Re-run calibration/backtest fixtures and document any threshold updates. |
| Low | Stateless calculator style | Open | Some calculators are stateless services; converting to utilities or formal injected services is a consistency decision. | Decide at the module level before broad refactoring. |

## Implemented Surface

Implemented today:

* Provider chain: `NoopMarketDataProvider`, `SchwabMarketDataProvider`,
  `YahooMarketDataProvider`, and `CompositeMarketDataProvider`.
* Schwab daily bars, intraday bars, and latest quotes with in-memory daily-bar
  caching and token-supplier abstraction.
* Yahoo chart fallback for daily, intraday, and quote data.
* Feature calculators for ATR, VWAP, relative volume, realized volatility,
  spread quality, liquidity consistency, directional persistence, rolling
  windows, multi-timeframe aggregation, intraday structure, liquidity texture,
  volatility expansion, replay, and stream processing.
* Shared `OhlcBar` contract in `lib-domain`, with `MarketBar` and `IntradayBar`
  using the same ATR implementation.
* Market-data snapshots emit raw features; normalized liquidity and volatility
  scores are filled by analytics-owned scoring.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| README layout | Module README now reflects actual packages and points shared bars/snapshots to `lib-domain`. |
| ATR duplication | `MarketFeatureCalculator` delegates to the shared ATR calculator instead of reimplementing it. |
| ATR definition | Default ATR uses Wilder smoothing with a compatibility arithmetic-mean helper retained. |
| Realized volatility | Uses log returns, sample variance, and an annualized overload. |
| Provider chain errors | Composite provider logs chained provider failures instead of swallowing them silently. |
| Determinism | Feature timestamps derive from latest bar context instead of `Instant.now()`. |
| Scoring boundary | Liquidity/volatility score policy moved to `lib-analytics`; this module emits raw features. |
| Schwab quotes | Empty Schwab quotes return unavailable results so fallback providers can run. |
| Schwab tests | `SchwabMarketDataProviderTest` covers major mapping and failure paths. |
| Sentinels and small math fixes | Degenerate numeric cases are documented and directional persistence loop naming was cleaned up. |

## Notes

The module does not choose trade direction, produce validation verdicts, persist
bars directly, track account state, or place orders. Schwab broker execution
belongs to the future `lib-execution` layer, not here.

Root tracking: [../ISSUES.md](../ISSUES.md).
