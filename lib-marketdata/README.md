# lib-marketdata

> **MD layer:** market-data half of **L2 (Data Ingestion)** plus the raw
> feature inputs that feed **L4 (Market-State Classification)** in
> `lib-analytics`. See [ARCHITECTURE.md](../ARCHITECTURE.md) for the full mapping.

`lib-marketdata` is responsible for retrieving market data and deriving the feature set used by analytics and validation.

This module should answer one question:

> What objective market context exists for this candidate right now?

It should not decide whether the trade is worth taking. It supplies clean, explainable features for downstream scoring.

## Responsibilities

- Retrieve price and volume data from one or more providers.
- Normalize provider-specific responses into internal market-bar models.
- Derive market features used by the analytics and validation layers.
- Keep provider adapters behind stable interfaces.
- Make feature calculations deterministic and testable.

Initial feature targets:

- latest close
- previous close
- daily range position
- average true range, or ATR
- liquidity / average volume
- VWAP proxy, if intraday VWAP is not available
- gap percentage
- distance from recent high / low

## Non-responsibilities

This library should not:

- Choose trade direction.
- Produce PASS / WATCH / REJECT verdicts.
- Classify macro or market regime by itself.
- Persist raw market bars directly unless routed through persistence contracts.
- Track account balances, positions, fills, or execution state.

## Expected flow

```text
candidate symbol
    -> market-data provider adapter
    -> normalized bars / quote data
    -> feature calculator
    -> market-data snapshot
    -> analytics / validation
```

## Design guidance

### Separate retrieval from calculation

Provider clients should only fetch and normalize data. Feature calculators should operate on internal models so they can be tested without network calls.

### Use deterministic calculations first

Before introducing complex indicators, build a small feature set that is transparent and easy to validate:

- ATR from recent daily bars
- range position from high / low / close
- liquidity from average volume
- gap percentage from previous close to current open or latest price

### Treat missing data as a first-class outcome

Bad or incomplete market data should not silently produce misleading scores. Return enough status detail for validation to reject or watch a candidate with a clear reason.

Possible data-quality flags:

- missing symbol
- insufficient history
- stale data
- illiquid symbol
- provider timeout
- split or corporate-action adjustment uncertainty

## Suggested package layout

```text
lib-marketdata
└── src/main/java/dev/reddragon/marketdata
    ├── provider        # external provider interfaces and adapters
    ├── model           # bars, quotes, snapshots, feature records
    ├── feature         # ATR, range position, liquidity, gap calculations
    └── quality         # stale/missing/insufficient data checks
```

## First implementation target

Start with end-of-day data before intraday data:

1. Define internal market bar and feature snapshot models.
2. Implement one provider adapter.
3. Fetch recent daily bars for a candidate ticker.
4. Calculate ATR, range position, and average volume.
5. Return a market-data snapshot with data-quality status.

## Testing expectations

Tests should cover:

- feature calculations using fixed bar fixtures
- insufficient lookback periods
- stale or missing bars
- provider response mapping
- provider failure handling
- numerical precision tolerances for derived indicators

Avoid tests that depend on live provider data. Use deterministic fixtures for calculation tests and mock provider clients for retrieval behavior.
