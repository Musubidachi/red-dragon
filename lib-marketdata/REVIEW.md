# lib-marketdata Review

Last updated: 2026-05-27

This review summarizes the current market-data module after the feature-math,
provider hardening, and VWAP session-boundary passes. Fixed audit detail is
compressed below the open work.

## Current Open Work

No open module-local issues are currently tracked.

Keep adding provider and builder branch tests as new edge cases are discovered.

## Implemented Surface

Implemented today:

* Provider chain: `NoopMarketDataProvider`, `SchwabMarketDataProvider`,
  `YahooMarketDataProvider`, and `CompositeMarketDataProvider`.
* Schwab daily bars, intraday bars, and latest quotes with in-memory daily-bar
  caching, status-aware retry/backoff, and token-supplier abstraction.
* Yahoo chart fallback for daily, intraday, and quote data.
* Feature calculators for ATR, cumulative and session-bound VWAP, relative
  volume, realized volatility, spread quality, liquidity consistency,
  directional persistence, rolling windows, multi-timeframe aggregation,
  intraday structure, liquidity texture, volatility expansion, replay, and
  stream processing.
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
| Schwab retry/backoff | Schwab HTTP calls retry only network failures, 429, and 5xx responses; non-429 4xx and malformed payloads fail without retry; retry delay uses capped jittered exponential backoff. |
| Schwab tests | `SchwabMarketDataProviderTest` covers major mapping and failure paths. |
| Yahoo and snapshot-builder tests | `YahooMarketDataProviderTest`, `LiquidityTextureSnapshotBuilderTest`, and `VolatilityExpansionSnapshotBuilderTest` cover provider mapping/failure paths plus additional builder edge cases. |
| VWAP session boundary | `VwapCalculator` exposes cumulative, explicit-session, and single-session APIs; intraday snapshots calculate VWAP for the latest US/Eastern session instead of folding prior sessions into the value. |
| Calculator style decision | Stateless calculators and snapshot builders remain service-style top-level classes so app/backtest wiring stays consistent and future configuration can be injected without another API shift. |
| Post-math validation | `mvn -pl lib-persistence,app -am "-Dnet.bytebuddy.experimental=true" test` passed marketdata, analytics, validation, backtest, persistence, and app tests after the ATR/realized-volatility changes. |
| Historical calibration fixture | `HistoricalMarketDataCalibrationFixtureTest` exercises fixed historical-style OHLCV bars through marketdata, analytics, validation, backtest, and calibration code paths for Wilder ATR, session VWAP, and realized volatility. |
| Sentinels and small math fixes | Degenerate numeric cases are documented and directional persistence loop naming was cleaned up. |

## Notes

The module does not choose trade direction, produce validation verdicts, persist
bars directly, track account state, or place orders. Dry-run broker execution
now lives in `lib-execution`; live Schwab execution is still deferred there.

Root tracking: [../ISSUES.md](../ISSUES.md).
