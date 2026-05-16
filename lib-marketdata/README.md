# lib-marketdata

> **MD layer:** market-data half of L2 plus raw feature inputs that feed L4 in
> `lib-analytics`. See [ARCHITECTURE.md](../ARCHITECTURE.md).

`lib-marketdata` retrieves market data and derives the feature set used by
analytics and validation.

## Responsibilities

* Retrieve price and volume data from providers.
* Normalize provider responses into internal market-bar models.
* Derive deterministic features for analytics and validation.
* Keep provider adapters behind stable interfaces.
* Treat missing or incomplete data as a first-class quality outcome.

## Current Provider Support

* `NoopMarketDataProvider` for disabled or unconfigured market data.
* `SchwabMarketDataProvider` for historical daily bars from Schwab
  `/pricehistory`, intraday bars from Schwab `/pricehistory`, and latest quotes
  from Schwab `/quotes`, with short-lived in-memory daily-bar caching and simple
  retry/backoff.
* `YahooMarketDataProvider` as an optional secondary chart provider for daily
  bars, intraday bars, and latest-price fallback.
* `CompositeMarketDataProvider` for ordered provider fallback.
* `SchwabAccessTokenSupplier` abstraction so `app` can provide OAuth-backed
  token rotation or fall back to a static configured access token.

## Current Feature Support

The implemented services cover ATR, VWAP, relative volume, realized volatility,
spread quality, liquidity consistency, directional persistence, rolling windows,
multi-timeframe aggregation, intraday structure, liquidity texture, volatility
expansion, and replay helpers.

## Non-Responsibilities

This library does not choose trade direction, produce PASS/WATCH/REJECT
verdicts, classify macro regime by itself, persist raw bars directly, track
account balances, track positions, or place orders.

## Current Package Layout

```text
lib-marketdata/src/main/java/dev/reddragon/marketdata
    models/      bars, snapshots, feature records, quality enum
    services/    feature calculators and aggregation services
    services/provider/ provider interface, noop provider, and composite fallback
    services/provider/schwab/ Schwab price-history adapter and DTOs
    services/provider/yahoo/ Yahoo chart fallback adapter
    config/      Schwab market-data and OAuth properties
    utilities/   math helpers
```

## Testing Expectations

Tests should cover feature calculations using fixed bar fixtures, insufficient
lookback periods, stale or missing bars, provider response mapping, provider
failure handling, and numerical precision tolerances. Avoid live provider data
in tests.

## App Endpoints

`app` exposes the configured provider chain through:

* `GET /api/market-data/{symbol}/daily`
* `GET /api/market-data/{symbol}/intraday`
* `GET /api/market-data/{symbol}/quote`
