# lib-marketdata — Implementation Review

**Date:** 2026-05-17
**Last fix pass:** 2026-05-20 (local fixes only) + 2026-05-24 (Wilder ATR + log-return / Bessel-corrected / annualized realized vol — both behaviours land; legacy arithmetic-mean ATR remains available via `arithmeticMeanTrueRange` for compatibility, but the default path is now Wilder-smoothed) + 2026-05-25 (shared `OhlcBar` contract in lib-domain; `MarketFeatureCalculator` now delegates ATR to `AverageTrueRangeCalculator` instead of reimplementing it inline — duplicate ATR math removed) + 2026-05-25 (transitional Finding #8: liquidity/volatility scoring thresholds extracted into `MarketScoringProperties`; defaults match prior baked-in values exactly, so the unconfigured deployment is a no-op) + 2026-05-25 (architectural Finding #8: scoring policy moved entirely to lib-analytics — `LiquidityScorer`, `VolatilityStabilityScorer`, `MarketDataSnapshotScorer`; lib-marketdata emits raw features with score=0 placeholders, the analytics enrichment step fills them in; ILLIQUID detection now uses a raw-feature `averageVolume < 100_000` check) + 2026-05-25 (Finding #14: `SchwabMarketDataProviderTest` covers 12 cases with `MockRestServiceServer`). Calibration drift expected on the ATR/vol math findings — see Open list.
**Reviewer:** Automated audit pass
**Scope:** `lib-marketdata/` — 28 source files: feature calculators, snapshot builders, provider chain (Schwab + Yahoo + Noop + Composite), config, replay/stream helpers, plus tests.

---

## Fix Status

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | High | README package layout drift (`models/`, `utilities/` don't exist) | **Fixed** — README now reflects actual on-disk layout |
| 2 | High | `MarketFeatureCalculator` reimplements ATR inline | **Fixed** — new `dev.reddragon.domain.models.OhlcBar` interface in lib-domain exposes `open() / high() / low() / close()`. `MarketBar` and `IntradayBar` both implement it (zero structural change — Lombok's fluent accessors already match the contract). `AverageTrueRangeCalculator.process` now takes `List<? extends OhlcBar>`, so the same algorithm serves daily-bar callers (`MarketFeatureCalculator`) and intraday-bar callers (`VolatilityExpansionSnapshotBuilder`) without duplication. The inline `averageTrueRange(List<MarketBar>)` + `trueRange(MarketBar, MarketBar)` helpers in `MarketFeatureCalculator` have been deleted; the public `averageTrueRange(bars)` is now a one-liner that delegates to the shared calculator. Daily series typically run with many more than 14 bars, so Wilder smoothing engages — calibration drift compounds with Finding #3 and is flagged at the top of this file. |
| 3 | High | ATR uses arithmetic mean, not Wilder smoothing | **Fixed** — `AverageTrueRangeCalculator.process(bars)` now uses Wilder smoothing with `DEFAULT_PERIOD = 14`. Seed = arithmetic mean of the first 14 true ranges; each subsequent bar evolves via `ATR_n = (ATR_{n-1} × 13 + TR_n) / 14`. Backwards-compatible: series with fewer than 14 TRs fall back to the arithmetic-mean variant (preserves the existing `atrIsAverageOfTrueRangesAcrossBars` test). Legacy behaviour explicitly available via `arithmeticMeanTrueRange(bars)`. New test `wilderSmoothingKicksInWhenSeriesLongEnough` pins the RMA arithmetic. Calibration drift expected for any thresholds previously tuned against the simple-mean output — flagged at the top of this file. |
| 4 | High | Realized vol uses population variance + no annualization | **Fixed** — `RealizedVolatilityCalculator.process(bars)` now uses (a) log returns instead of arithmetic returns, (b) Bessel-corrected sample variance (divide by `N − 1`), and (c) a new `processAnnualized(bars, barsPerYear)` overload that scales by `√barsPerYear`. `TRADING_DAYS_PER_YEAR = 252` and `FIVE_MINUTE_BARS_PER_SESSION = 78` constants exposed for common scaling. New `RealizedVolatilityCalculatorTest` covers the math, the annualization invariant, defensive zero-close handling, and the `N=1` boundary (returns 0 instead of NaN). Calibration drift expected — flagged at the top of this file. |
| 5 | Medium | `CompositeMarketDataProvider` swallows exceptions silently | **Fixed** — `@Slf4j` + `log.warn` on each chained provider failure |
| 6 | Medium | `Instant.now()` in `MarketFeatureCalculator` | **Fixed** — uses `latestBar.date().atTime(21:00).toInstant(UTC)` for deterministic replay |
| 7 | Medium | No Lombok service style | Open — calculators are stateless; would need `@UtilityClass` conversion |
| 8 | Medium | Magic thresholds in `liquidityScore`/`volatilityStabilityScore` | **Fixed (full architectural move)** — scoring policy now lives in lib-analytics (L4) where it belongs. New `analytics/config/MarketScoringProperties` (moved from lib-marketdata.config), `analytics/services/marketscoring/LiquidityScorer`, `VolatilityStabilityScorer`, and `MarketDataSnapshotScorer` (composes the two). lib-marketdata's `MarketFeatureCalculator` no longer computes the scores — emits `0.0` placeholders on the snapshot; `CandidatePipelineOrchestrator` and `BacktestReplayEngine` apply `MarketDataSnapshotScorer.process(rawSnapshot)` immediately downstream so every consumer (analytics, validation, persistence) sees the enriched values. ILLIQUID quality detection switched to a raw-feature check (`averageVolume < ILLIQUID_VOLUME_FLOOR = 100_000`) since it no longer needs the bucket math. New `MarketDataSnapshot.withScores(double, double)` helper on lib-domain makes the enrichment a clean immutable copy. New `MarketDataSnapshotScorerTest` locks down the numeric outputs (1.00 / 0.15 buckets; 0.90 stability for low ATR%; 0.50 sentinel for undefined ATR%). Unconfigured deployment is a behavioural no-op vs. the pre-move output. |
| 9 | Low | `directionalPersistence` off-by-one + misleading variable names | **Fixed** — loop starts at `i = 2`, renamed to `recentUp`/`olderUp` |
| 10 | Low | Inconsistent missing-data sentinels | **Fixed (documented)** — each sentinel now has javadoc explaining its choice; values unchanged for backwards compat |
| 11 | Medium | Linear retry/backoff + retries all RuntimeExceptions | Open — Schwab integration is doc-only/deferred per project memory |
| 12 | Low | Schwab double-wraps `IllegalStateException` | **Fixed** — outer catch rethrows `IllegalStateException` unwrapped |
| 13 | Low | Schwab quote returns synthetic `EMPTY_BARS` instead of unavailable | **Fixed** — returns `MarketQuote.unavailable(...)` so composite chain falls back |
| 14 | Medium | Test coverage gap (3 of 6 areas) | **Partially fixed** — new `SchwabMarketDataProviderTest` covers the longest file in the module with 12 tests using `MockRestServiceServer`: happy-path historical bars (mapping + sort + Authorization header), empty candles array, malformed JSON, 5xx exhaust-retry, 429 exhaust-retry (pins current behaviour even though Finding #11 wants HTTP-status-aware retry), quote with `lastPrice` / `mark` / `closePrice` fallback chain, quote unavailable when all price fields are zero (Finding #13), unconfigured short-circuit (no HTTP fires), blank-symbol boundary rejection. `spring-test` added to lib-marketdata pom. Yahoo provider + snapshot builders still uncovered. |
| 15 | Medium | VWAP has no session boundary | Open — needs API change (session date param) |

Also added `slf4j-api` to `lib-marketdata/pom.xml` (required by Finding #5 fix). README now describes the full feature-calculator + snapshot-builder + provider chain layout including the previously-undocumented `services/replay/` and `services/stream/` packages.

**Open: 3** — no Lombok service style (#7), Schwab retry/backoff hardening (#11 — deferred until live trading), VWAP session boundary (#15 — API-breaking change). Math findings (#2, #3, #4) and the architectural #8 move are resolved; Schwab provider tests now cover the critical paths (#14 partial — Yahoo + snapshot builders remain uncovered). Downstream calibration thresholds should be re-validated against the unified Wilder output.

---

---

## Expected Behavior (per docs)

`lib-marketdata/README.md` is the in-module spec. Key claims:

- L2 market-data half + raw feature inputs that feed L4 (line 3–4).
- Retrieve, normalize, derive features, keep providers behind stable interfaces, treat missing data as first-class quality outcome (lines 11–15).
- Schwab is the primary provider with in-memory caching and retry/backoff; Yahoo is a fallback; `CompositeMarketDataProvider` chains them in order (lines 19–28).
- Implemented features: ATR, VWAP, relative volume, realized volatility, spread quality, liquidity consistency, directional persistence, rolling windows, multi-timeframe aggregation, intraday structure, liquidity texture, volatility expansion, replay helpers (lines 31–35).
- Non-responsibilities: no trade direction, no PASS/WATCH/REJECT verdicts, no macro regime, no persistence, no portfolio, no orders (lines 39–41).
- **Package layout (README lines 43–54):**

```
lib-marketdata/src/main/java/dev/reddragon/marketdata
    models/      bars, snapshots, feature records, quality enum
    services/    feature calculators and aggregation services
    services/provider/ provider interface, noop provider, and composite fallback
    services/provider/schwab/ Schwab price-history adapter and DTOs
    services/provider/yahoo/ Yahoo chart fallback adapter
    config/      Schwab market-data and OAuth properties
    utilities/   math helpers
```

- Testing: feature calculations on fixed bar fixtures, insufficient lookback, stale/missing bars, provider mapping, provider failure, numerical precision tolerances. Avoid live provider data (lines 56–61).

Project-wide conventions from `../ARCHITECTURE.md`:

- "Provider adapters stay sealed. Schwab DTOs are package-private and never leak past `lib-marketdata/services/provider/schwab/`." (line 137–139)
- `@Value` + fluent accessors for value objects.
- `@RequiredArgsConstructor` + `@Slf4j` for services.
- No nested classes.

---

## Files Reviewed

| Category | Files |
| --- | --- |
| Feature calculators | `services/MarketFeatureCalculator.java`, `services/VwapCalculator.java`, `services/AverageTrueRangeCalculator.java`, `services/RealizedVolatilityCalculator.java`, `services/RelativeVolumeCalculator.java`, `services/DirectionalPersistenceCalculator.java`, `services/LiquidityConsistencyCalculator.java`, `services/SpreadQualityCalculator.java`, `services/RollingWindowService.java`, `services/MultiTimeframeAggregationService.java` |
| Snapshot builders | `services/IntradayStructureSnapshotBuilder.java`, `services/LiquidityTextureSnapshotBuilder.java`, `services/VolatilityExpansionSnapshotBuilder.java` |
| Replay / stream | `services/replay/MarketReplayService.java`, `services/stream/MarketDataStreamProcessor.java` |
| Provider interface + chain | `services/provider/MarketDataProvider.java`, `services/provider/NoopMarketDataProvider.java`, `services/provider/CompositeMarketDataProvider.java` |
| Schwab adapter (package-private DTOs) | `services/provider/schwab/SchwabMarketDataProvider.java`, `SchwabAccessTokenSupplier.java`, `StaticSchwabAccessTokenSupplier.java`, `SchwabCandle.java`, `SchwabPriceHistoryResponse.java`, `SchwabBarCacheEntry.java` |
| Yahoo adapter | `services/provider/yahoo/YahooMarketDataProvider.java` |
| Config | `config/SchwabMarketDataProperties.java`, `config/SchwabOAuthProperties.java`, `config/YahooMarketDataProperties.java` |
| Package-info | `package-info.java` (one file, top-level only) |
| Tests | 6 files |

---

## Findings

### 1. Documented package layout diverges from on-disk layout — `models/` and `utilities/` directories don't exist

**Severity:** High
**Files:** `lib-marketdata/README.md:43-54` vs. actual filesystem.

**Expected vs Actual:** README claims:

```
models/      bars, snapshots, feature records, quality enum
utilities/   math helpers
```

Actual: neither `lib-marketdata/src/main/java/dev/reddragon/marketdata/models/` nor `.../utilities/` exists. `Glob "C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\utilities\**"` returns "No files found"; same for `models/`. There is no `MarketBar.java` or `IntradayBar.java` inside lib-marketdata at all — those live in `lib-domain/src/main/java/dev/reddragon/domain/models/` (verified). The README's "models/ bars, snapshots, feature records, quality enum" line refers to types that are not in this module.

**Why it matters:** Two problems:

1. **Onboarding misdirection.** A junior dev reading the README to find `MarketBar` will look in lib-marketdata, find nothing, and have to grep across modules. ARCHITECTURE.md line 119 calls this file out as part of the recommended reading order — if the README sends readers to the wrong place, the convention is more confusing than useful.
2. **The "math helpers" claim is also wrong.** Math helpers all live in `lib-math`. The README implies lib-marketdata has its own utilities, which would also be a layering violation if true.

**Proposed fix:** Update README "Current Package Layout" to reflect reality:

```
lib-marketdata/src/main/java/dev/reddragon/marketdata
    services/    feature calculators and aggregation services
    services/provider/ provider interface, noop, composite fallback
    services/provider/schwab/ Schwab adapter (package-private DTOs)
    services/provider/yahoo/ Yahoo adapter
    services/replay/ deterministic replay helpers
    services/stream/ market-data stream processing
    config/      Schwab / Yahoo / OAuth properties
```

And note that bar/snapshot value objects live in `lib-domain`.

---

### 2. `MarketFeatureCalculator` reinvents ATR inside itself rather than delegating to `AverageTrueRangeCalculator`

**Severity:** High
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:209-229`
- `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\AverageTrueRangeCalculator.java:16-44`

**Expected vs Actual:** README lists ATR as a documented feature service. Actual: `AverageTrueRangeCalculator` exists as a dedicated service, but `MarketFeatureCalculator.averageTrueRange(bars)` (line 209–219) computes its own ATR inline:

```java
// MarketFeatureCalculator.java:209-219 — inline reimplementation
private double averageTrueRange(List<MarketBar> bars) {
    double total = 0.0;
    int count = 0;
    for (int index = 1; index < bars.size(); index++) {
        total += trueRange(bars.get(index), bars.get(index - 1));
        count++;
    }
    return count == 0 ? 0.0 : total / count;
}
```

vs.

```java
// AverageTrueRangeCalculator.java:16-33 — the dedicated service
public double process(List<IntradayBar> bars) {
    Objects.requireNonNull(bars, "bars are required");
    if (bars.size() < 2) {
        return 0.0;
    }
    double total = 0.0;
    for (int index = 1; index < bars.size(); index++) {
        IntradayBar current = bars.get(index);
        IntradayBar previous = bars.get(index - 1);
        total += trueRange(current, previous);
    }
    return total / (bars.size() - 1);
}
```

Same algorithm. Worse, the type signature differs (`List<MarketBar>` vs `List<IntradayBar>`), which is why the inline copy was written, but `IntradayBar` and `MarketBar` carry essentially the same OHLC fields. The proper fix is to extract the algorithm to operate on a common interface or to take a `BinaryOperator<Double>` that pulls the relevant fields. ATR for *daily* bars also conventionally uses Wilder smoothing — see Finding #3.

**Why it matters:** The single biggest source of correctness drift in market data is feature definitions. Two parallel implementations of "the ATR" mean two different numbers can be produced for the same input depending on which entry point a caller used. Calibration (L8) will report drift that is actually a feature-definition mismatch.

**Proposed fix:** Extract a shared interface `OhlcBar { double high(); double low(); double close(); }` in lib-domain (or use sealed types), have both `MarketBar` and `IntradayBar` implement it, then have `MarketFeatureCalculator` call `AverageTrueRangeCalculator.process(bars)` instead of reimplementing.

---

### 3. ATR uses a simple arithmetic mean rather than Wilder's smoothing or any windowed lookback

**Severity:** High
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\AverageTrueRangeCalculator.java:16-33`
- `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:209-219`

**Expected vs Actual:** README line 31 lists "ATR" without specifying the variant. Industry convention for ATR is **Wilder's RMA**:

```
ATR_n = ((ATR_{n-1} × (n-1)) + TR_n) / n     where n = 14
```

Actual: a simple arithmetic mean of all true ranges in the supplied list, with **no lookback window**:

```java
return total / (bars.size() - 1);
```

If a caller passes 200 bars, ATR is the mean over all 199 ranges — not the conventional 14-bar exponentially-smoothed value. The volatility-stability score (`MarketFeatureCalculator.volatilityStabilityScore(latestClose, averageTrueRange)`, line 247–268) then thresholds `atrPercent` to produce a 0..1 score — those thresholds were presumably calibrated against a windowed ATR.

**Why it matters:** Two issues stack:

1. **A 200-bar average dilutes recent volatility expansion.** Wilder smoothing gives more weight to recent bars; the arithmetic mean does not. A regime shift today is invisible in the ATR until many bars later.
2. **No lookback parameter.** Caller has no way to ask for "the ATR over the last 14 days." They must pre-slice the list, which (a) puts the windowing concern in every caller and (b) is not what the README claims the service does.

**Proposed fix:** Implement Wilder smoothing with a configurable lookback (default 14). Add a `process(List<? extends OhlcBar> bars, int period)` overload. Update `volatilityStabilityScore` thresholds if they were calibrated against the old arithmetic mean.

---

### 4. `RealizedVolatilityCalculator` uses population variance (divide by N) instead of sample variance (N-1), and does not annualize

**Severity:** High
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\RealizedVolatilityCalculator.java:59-75`

**Expected vs Actual:** README line 31 lists "realized volatility" as a feature without specifying. Industry standard (e.g., Sharpe ratio inputs, options pricing, regime classifiers) uses:

- **Sample variance** (divide by `N - 1`, Bessel's correction).
- **Annualized** volatility: `σ_period × √(periods per year)`. For 5-minute bars, that's `× √(252 × 78)`; for daily, `× √252`.

Actual:

```java
return Math.sqrt(varianceTotal / values.size());   // population variance
```

No annualization. The output is the per-bar standard deviation of arithmetic returns.

**Why it matters:** Callers that compare this to a "10% annualized vol" threshold will compare wrong-units numbers. Per-bar std-dev of intraday returns is typically `~0.001`; annualized vol of the same series is typically `~0.20`. Off by two orders of magnitude. Downstream `volatilityStabilityScore` and regime classifiers consume this — see lib-analytics.

**Proposed fix:** Either:
- Use `Math.sqrt(varianceTotal / (values.size() - 1))` and document the unit clearly as "per-bar std dev of arithmetic returns," **or**
- Add an `annualize(double perBarVol, int barsPerYear)` helper and have callers explicitly pick the unit, **or**
- Take an `int barsPerYear` parameter and return annualized vol directly.

Also: arithmetic returns vs **log returns** is another finance-standard choice. Log returns are time-additive; arithmetic returns are not. For realized vol the difference is small but non-zero.

---

### 5. `CompositeMarketDataProvider` silently swallows `RuntimeException` from every chained provider — no logging

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\provider\CompositeMarketDataProvider.java:26-67`

**Expected vs Actual:** README line 15: "Treat missing or incomplete data as a first-class quality outcome." Actual: provider failures are caught and ignored without any record:

```java
for (MarketDataProvider provider : providers) {
    try {
        List<MarketBar> bars = provider.historicalDailyBars(symbol, from, to);
        if (bars != null && !bars.isEmpty()) {
            return bars;
        }
    } catch (RuntimeException ignored) {
        // Try the next provider in the chain.
    }
}
return List.of();
```

The `catch (RuntimeException ignored)` block has no `log.warn(...)` and no metric increment. If Schwab is down and the fallback returns the Yahoo data, no one knows the primary failed.

**Why it matters:** Operationally invisible failures. The L4 regime classifier will trust Yahoo data; the L8 calibration loop will see "wins" attributed to a regime that was actually computed off the fallback. Diagnosing "why is win rate dropping" requires knowing which provider produced the data — and right now no signal is captured. Project memory `project_red_dragon_known_gaps.md` mentions "metrics" as implemented in Tier 3; this catch site is one of the gaps.

**Proposed fix:** Add `@Slf4j` and `log.warn("Provider {} failed for {}; trying next", provider.providerName(), symbol, e)` inside each catch. Increment a Micrometer counter `marketdata.provider.fallback{provider=name}` when a fallback fires. Verify the catch handles all subclasses of `RuntimeException` intentionally — `IllegalArgumentException` (e.g., bad symbol) should probably *not* be retried via fallback; only network/HTTP errors should.

---

### 6. `MarketFeatureCalculator.buildSnapshot` calls `Instant.now()` — same determinism issue as lib-domain Finding #5

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:75-91, 147-160`

**Expected vs Actual:** Two `Instant.now()` calls (line 77 for the live path, line 154 for the empty-snapshot path). Backtest determinism requires that replaying the same bars produce the same `MarketDataSnapshot`. Today, `BacktestReplayEngine` runs `MarketFeatureCalculator.process(symbol, bars)` and stamps each snapshot with the wall clock at replay time. Two backtest runs of the same fixture produce two non-equal `MarketDataSnapshot.observedAt` values.

**Why it matters:** lib-backtest README line 20: "identical inputs must produce identical outputs." This is the upstream of that promise. `equals/hashCode` of `MarketDataSnapshot` depend on `observedAt`, so `BacktestReport` is not byte-equal across runs.

**Proposed fix:** Inject a `Clock` via constructor:

```java
@RequiredArgsConstructor
public class MarketFeatureCalculator {
    private final Clock clock;
    public MarketFeatureCalculator() { this(Clock.systemUTC()); }
    // ...
    private MarketDataSnapshot emptySnapshot(...) {
        return new MarketDataSnapshot(symbol, clock.instant(), ...);
    }
}
```

Backtest then wires a `Clock.fixed(...)` or derives `observedAt` from the latest bar's date.

---

### 7. `MarketFeatureCalculator` is not a Lombok service — manual everything

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:16-269`

**Expected vs Actual:** ARCHITECTURE.md line 130: `@RequiredArgsConstructor`, `@Slf4j` are the defaults. Actual: `MarketFeatureCalculator` is a 269-line class with zero dependencies (it has no `private final` fields), no Lombok annotations, no logging. It's an effectively-static service masquerading as instantiable. The same pattern appears in `VwapCalculator`, `AverageTrueRangeCalculator`, `RealizedVolatilityCalculator`, and the snapshot builders.

```java
public class MarketFeatureCalculator {
    // no fields, no constructor
    public MarketDataSnapshot process(String symbol, List<MarketBar> bars) {
```

**Why it matters:** Two follow-ons:
1. The calculators *could* be `@UtilityClass` like lib-math's helpers, since they hold no state.
2. Or they *should* take dependencies via constructor (e.g., `Clock` for Finding #6, configurable thresholds for Finding #11) and then become real services.

Today they sit in a half-way state: instantiated as if they were stateful, but stateless internally. The test code creates a fresh instance per test method, which is wasteful and signals the design ambiguity.

**Proposed fix:** Decide. If they hold no state and never will, make them `@UtilityClass` and call them statically. If they take a `Clock` per Finding #6 or thresholds per Finding #11, give them proper dependencies and Lombok them.

---

### 8. `MarketFeatureCalculator.liquidityScore` and `volatilityStabilityScore` use hardcoded magic-number step functions

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:231-268`

**Expected vs Actual:**

```java
private double liquidityScore(double averageVolume) {
    if (averageVolume >= 5_000_000) return 1.0;
    if (averageVolume >= 1_000_000) return 0.8;
    if (averageVolume >= 500_000)   return 0.6;
    if (averageVolume >= 100_000)   return 0.35;
    return 0.15;
}

private double volatilityStabilityScore(double latestClose, double averageTrueRange) {
    if (latestClose <= 0 || averageTrueRange <= 0) return 0.5;
    double atrPercent = averageTrueRange / latestClose;
    if (atrPercent <= 0.03) return 0.9;
    if (atrPercent <= 0.06) return 0.75;
    if (atrPercent <= 0.10) return 0.55;
    if (atrPercent <= 0.15) return 0.35;
    return 0.15;
}
```

The thresholds (`5M`, `1M`, `500k`, `100k` for volume; `0.03`, `0.06`, `0.10`, `0.15` for ATR%) are baked in. lib-validation owns thresholds in `ValidationThresholds`; lib-analytics is documented to be a pure deterministic scorer. lib-marketdata is supposed to be raw feature extraction, not score generation — but this is a scorer in disguise.

**Why it matters:** Two architectural concerns:
1. **Crosses an MD-layer boundary.** Producing a normalized 0..1 `liquidityScore` is L2-feeds-L4 logic; producing the *score* belongs in L4 (lib-analytics/classification/`LiquidityTextureScorer`). lib-marketdata should expose `averageVolume` (a feature) and let lib-analytics translate to a score.
2. **Untunable.** No validation profile can shift the liquidity thresholds without a code change.

**Proposed fix:** Move both score functions to lib-analytics (probably `LiquidityTextureScorer` and `VolatilityExpansionScorer`). `MarketDataSnapshot.liquidityScore` becomes a derived field set by the analytics step rather than the marketdata step. If keeping in lib-marketdata for transitional reasons, at minimum extract the constants to `MarketDataProperties` / `ValidationThresholds` so they're tunable.

---

### 9. `MarketFeatureCalculator.directionalPersistence` has a one-iteration off-by-one waste

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:191-207`

**Expected vs Actual:**

```java
private double directionalPersistence(List<MarketBar> bars) {
    if (bars.size() < 2) return 0.5;
    int consistent = 0;
    int total = 0;
    for (int i = 1; i < bars.size(); i++) {
        double prev = bars.get(i - 1).close();
        double curr = bars.get(i).close();
        if (i >= 2) {
            double prevPrev = bars.get(i - 2).close();
            boolean prevUp = curr > prev;
            boolean currUp = prev > prevPrev;
            if (prevUp == currUp) consistent++;
            total++;
        }
    }
    return total == 0 ? 0.5 : (double) consistent / total;
}
```

The `i == 1` iteration computes `prev` and `curr` but then skips the body because of `if (i >= 2)`. The loop should start at `i = 2`. Also, `boolean prevUp = curr > prev` and `boolean currUp = prev > prevPrev` have the variable names backwards — `prevUp` is actually "the most recent direction" and `currUp` is "the older direction." It still works because the comparison is symmetric, but the naming is misleading.

**Why it matters:** Style and correctness review: anyone reading this needs to triple-check the naming to realize it's correct. Easy to introduce a real bug if extended (e.g., adding a magnitude weight).

**Proposed fix:** Start at `i = 2`, rename to clarify:

```java
for (int i = 2; i < bars.size(); i++) {
    boolean recentUp = bars.get(i).close()    > bars.get(i - 1).close();
    boolean olderUp  = bars.get(i - 1).close() > bars.get(i - 2).close();
    if (recentUp == olderUp) consistent++;
    total++;
}
```

---

### 10. `MarketFeatureCalculator.rangePosition` returns `0.5` when range is zero, but `vwapDeviation` returns `0.0` for the analogous degenerate case — inconsistent "no data" sentinel

**Severity:** Low
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:106-112` (rangePosition → 0.5)
- `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:166-169` (relativeVolume → 1.0)
- `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\MarketFeatureCalculator.java:175-185` (vwapDeviation → 0.0)
- `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\VwapCalculator.java:26-28` (vwap → 0.0)

**Expected vs Actual:** Different degenerate-case defaults across siblings:
- `rangePosition` for zero range → `0.5` (midpoint)
- `relativeVolume` for zero avg volume → `1.0` ("average")
- `vwapDeviation` for zero volume → `0.0` (no deviation)
- `vwap` (raw) for zero volume → `0.0` (price-of-nothing)

Each individually is reasonable. As a set they're inconsistent and there's no documented convention.

**Why it matters:** The downstream scorer cannot tell "0.5" from "rangePosition is genuinely mid-range" vs "we have no data to compute it." Same for `1.0 relativeVolume`. Calibration will treat these synthetic values as real signal.

**Proposed fix:** Pick one approach. Either (a) return `MarketDataQuality.INSUFFICIENT_HISTORY` (already an enum value) for any field that can't be computed, propagating absence through a wrapper, or (b) document each sentinel in javadoc and ensure no two fields collide on the same numeric placeholder.

---

### 11. `SchwabMarketDataProvider.fetchBarsWithRetry` treats all `RuntimeException` as retryable and uses linear-multiplicative backoff (`attempt × backoffMs`), not exponential

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\provider\schwab\SchwabMarketDataProvider.java:169-194`

**Expected vs Actual:** README line 22–23 says "simple retry/backoff." Actual:

```java
private void sleepBackoff(int attempt) {
    long sleepMs = Math.max(0L, properties.getRetryBackoffMillis()) * attempt;
    if (sleepMs == 0L) return;
    try {
        Thread.sleep(sleepMs);
    } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
    }
}
```

Two issues:

1. **Linear, not exponential.** `attempt × backoffMs` is linear; convention is `backoffMs × 2^(attempt-1)`. With `backoffMs = 500` and 3 retries, this sleeps 500, 1000, 1500 ms — not 500, 1000, 2000 ms. Difference is small at 3 retries, but if `maxRetries` grows the tail is much longer with exponential.
2. **All RuntimeException retried.** `IllegalArgumentException` (programmer error) gets retried. Network errors and bad-input errors should be distinguished. Also no jitter, so multiple simultaneous failures will retry in lockstep (thundering herd).

The lib-ingestion review notes a similar pattern for SEC; this is the second instance.

**Why it matters:** Operationally, retries hide real errors and amplify them at scale. Also: no special handling for 429 (rate-limited) vs 500 (server error) — the project memory mentions Schwab integration is doc-only / planned. When it goes live, these gaps will surface.

**Proposed fix:** Switch to exponential backoff with jitter. Catch `RestClientException` specifically and let `IllegalArgumentException` propagate. Add response-code awareness: parse the HTTP status when the wrapped exception is `HttpClientErrorException`, retry only on 429/5xx, propagate 4xx (other than 429) immediately.

---

### 12. `SchwabMarketDataProvider` wraps Schwab errors twice (`IllegalStateException` of `IllegalStateException`)

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\provider\schwab\SchwabMarketDataProvider.java:196-216`

**Expected vs Actual:**

```java
private List<MarketBar> fetchBars(String symbol, LocalDate from, LocalDate to) {
    try {
        return toMarketBars(symbol, fetchPriceHistory(buildUri(symbol, from, to)));
    } catch (Exception error) {
        throw new IllegalStateException("Failed to retrieve Schwab market data for " + symbol, error);
    }
}

private List<SchwabCandle> fetchPriceHistory(URI uri) {
    try {
        ...
    } catch (Exception error) {
        throw new IllegalStateException("Failed to retrieve Schwab price history", error);
    }
}
```

If `fetchPriceHistory` throws `IllegalStateException("Failed to retrieve Schwab price history", error)`, `fetchBars` wraps it again: `IllegalStateException("Failed to retrieve Schwab market data for AAPL", IllegalStateException("Failed to retrieve Schwab price history", originalError))`. Three layers of nesting per failure.

**Why it matters:** Log spew. Stack-trace forensics requires walking two `caused by` levels to get to the root cause.

**Proposed fix:** Drop the inner wrapping or make the outer catch only catch non-`IllegalStateException` types and re-throw without wrapping for the inner case.

---

### 13. `SchwabMarketDataProvider.quote` accepts a successful HTTP response with no usable price and returns a synthetic `EMPTY_BARS` quote rather than failing the chain

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\provider\schwab\SchwabMarketDataProvider.java:106-139`

**Expected vs Actual:**

```java
double last = firstPositive(
        quote.path("lastPrice").asDouble(0.0),
        quote.path("mark").asDouble(0.0),
        quote.path("closePrice").asDouble(0.0)
);
return new MarketQuote(
        normalized,
        quoteInstant(quote),
        last,
        ...,
        last > 0.0 ? MarketDataQuality.COMPLETE : MarketDataQuality.EMPTY_BARS,
        last > 0.0 ? List.of("Provider: Schwab") : List.of("Schwab quote did not include a usable last price.")
);
```

When Schwab returns a quote with no `lastPrice`, `mark`, or `closePrice`, the provider returns a "complete" `MarketQuote` with `quality = EMPTY_BARS`. The composite chain then sees `quote.available()` — depending on how `MarketQuote.available()` is implemented, the fallback may or may not fire. Specifically: a "successful" call to a primary provider that returned no data should fall through to the fallback, not return.

**Why it matters:** Subtle silent-failure path. If `available()` returns `true` for `EMPTY_BARS` quality, the fallback doesn't fire and the consumer gets a fake quote.

**Proposed fix:** Either return `MarketQuote.unavailable(symbol, "...")` when no price field is positive, or guarantee `MarketQuote.available()` returns false for `EMPTY_BARS` quality. Verify the contract in `lib-domain.MarketQuote`.

---

### 14. Test coverage diverges from README "Testing Expectations"

**Severity:** Medium
**Files:**
- `lib-marketdata\src\test\java\dev\reddragon\marketdata\config\SchwabMarketDataPropertiesTest.java`
- `services\provider\CompositeMarketDataProviderTest.java`
- `services\MarketFeatureCalculatorTest.java`
- `services\VwapCalculatorTest.java`
- `services\AverageTrueRangeCalculatorTest.java`
- `services\IntradayStructureSnapshotBuilderTest.java`

**Expected vs Actual:** README lines 56–61 list six expected coverage areas:

1. Feature calculations on fixed bar fixtures — ✓ (VwapCalculatorTest, AverageTrueRangeCalculatorTest, MarketFeatureCalculatorTest)
2. Insufficient lookback periods — Partial (smoke only)
3. Stale or missing bars — ✗ (no test names "stale" or "missing")
4. Provider response mapping — ✗ (no SchwabMarketDataProviderTest, no YahooMarketDataProviderTest)
5. Provider failure handling — ✓ (CompositeMarketDataProviderTest)
6. Numerical precision tolerances — Unclear; tests use `assertEquals(double, double, delta)` inconsistently.

`SchwabMarketDataProvider` (the longest and most complex file in the module) has zero direct tests. `YahooMarketDataProvider`, the three snapshot builders (`LiquidityTextureSnapshotBuilder`, `VolatilityExpansionSnapshotBuilder` — only `IntradayStructureSnapshotBuilder` has a test), and the replay/stream services are also untested.

**Why it matters:** The provider layer is the production fault line. Token refresh, rate-limit responses, partial JSON, candle-array empty cases — all are unverified. The README explicitly says "Avoid live provider data in tests" but does not say "skip the provider entirely."

**Proposed fix:** Add `SchwabMarketDataProviderTest` with `MockRestServiceServer` (Spring Boot test) stubs for each endpoint, covering happy path, 429, 500, empty candles, malformed JSON. Same for Yahoo.

---

### 15. `VwapCalculator` has no session-boundary awareness; misuse will silently produce a multi-day VWAP

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-marketdata\src\main\java\dev\reddragon\marketdata\services\VwapCalculator.java:16-31`

**Expected vs Actual:** Conventionally, VWAP is *intraday* and resets at session open. The class accepts a `List<IntradayBar>` with no session boundary parameter:

```java
public double process(List<IntradayBar> bars) {
    Objects.requireNonNull(bars, "bars are required");
    if (bars.isEmpty()) return 0.0;
    double weightedPriceTotal = weightedPriceTotal(bars);
    long volumeTotal = volumeTotal(bars);
    if (volumeTotal == 0) return 0.0;
    return weightedPriceTotal / volumeTotal;
}
```

A caller passing 3 trading days worth of intraday bars will get one VWAP that spans all three sessions. There is no javadoc warning, no enforcement, and no overload that takes a session/date range.

**Why it matters:** The downstream `MarketFeatureCalculator.vwapDeviation` uses *daily* bars (line 175–185, reimplemented), which is arguably correct for the "where is the close relative to the cumulative VWAP" feature. But the standalone `VwapCalculator.process(intradayBars)` is exposed to other code (e.g., `app` or future tests) and trivially misusable.

**Proposed fix:** Either (a) make `process` take a `LocalDate session` and filter bars to that session, or (b) split into `intradaySessionVwap(LocalDate, List<IntradayBar>)` and `multiDayCumulativeVwap(List<IntradayBar>)` with names that say what they do.

---

## Schwab DTO Leakage Audit

ARCHITECTURE.md line 137: "Schwab DTOs are package-private and never leak past `lib-marketdata/services/provider/schwab/`."

| Type | Visibility | Leaks? |
| --- | --- | --- |
| `SchwabCandle` | package-private (no `public` modifier on `record SchwabCandle`) | ✓ Sealed |
| `SchwabPriceHistoryResponse` | package-private | ✓ Sealed |
| `SchwabBarCacheEntry` | package-private | ✓ Sealed |
| `SchwabMarketDataProvider` | `public` (correct — it implements the public `MarketDataProvider` interface) | ✓ |
| `SchwabAccessTokenSupplier` | `public` (interface, intentional) | ✓ |
| `StaticSchwabAccessTokenSupplier` | (verify) | (not reviewed in depth) |

The DTO sealing convention is honored. ✓

---

## Doc/Code Drift Summary

| README claim | Reality |
| --- | --- |
| `models/` directory exists | ✗ (Finding #1) |
| `utilities/` directory exists | ✗ (Finding #1) |
| ATR feature | Partially — implemented but simple-mean, not Wilder (Finding #3) |
| Realized vol feature | Partially — implemented as per-bar std dev, not annualized (Finding #4) |
| Treat missing data as first-class quality outcome | Partially — `MarketDataQuality` enum exists, but `CompositeMarketDataProvider` swallows errors silently (Finding #5) |
| `app` endpoints `GET /api/market-data/{symbol}/daily|intraday|quote` | Not in lib (lives in `app`) — out of scope here, but consistent with README. |
| Testing expectations | 3 of 6 covered (Finding #14) |

---

## Strengths

1. **Schwab DTO sealing is correctly enforced.** All three Schwab DTOs are package-private records. ARCHITECTURE.md convention is honored end-to-end.
2. **Provider interface is clean.** `MarketDataProvider` has three methods (`historicalDailyBars`, `intradayBars`, `quote`) plus `providerName`. The three concrete implementations (Schwab, Yahoo, Noop) plus the composite cover the README claims.
3. **`@JsonIgnoreProperties(ignoreUnknown = true)` on both Schwab DTOs.** Schwab adds optional fields without warning; this prevents deserialization breakage on minor API additions.
4. **Caching is bounded and short-lived.** `SchwabBarCacheEntry.expired(ttlSeconds)` properly honors `<= 0` as "disabled" (line 22). `ConcurrentHashMap` is the right choice for the shared cache.
5. **Token rotation abstraction.** `SchwabAccessTokenSupplier` is an interface; `StaticSchwabAccessTokenSupplier` is the no-rotation fallback. `app` can plug in an OAuth-backed supplier without touching the provider. Good separation.
6. **Multiple constructor overloads on Schwab provider** allow callers to inject either a custom `RestClient`/`ObjectMapper` (tests) or the default (production). Defaults compose cleanly.
7. **`Comparator.comparing(MarketBar::date).sort` on every output** ensures downstream code receives chronologically-ordered bars regardless of provider.
8. **`MarketFeatureCalculator` outputs a `MarketDataQuality` enum** (MISSING_SYMBOL, EMPTY_BARS, INSUFFICIENT_HISTORY, ILLIQUID, COMPLETE) — README's "treat missing as first-class outcome" is at least represented in the type system, even though Finding #5 shows the chain layer doesn't propagate failures distinctly.
9. **Pom dependencies are minimal.** Only lib-domain, lib-math, spring-web (for `RestClient`), jackson-databind, lombok, junit. No persistence, no Spring Boot starter, no test-containers. Library jar is clean.
10. **No portfolio state, no order placement.** README non-responsibilities are honored.

---

## Summary table — issue count by severity

| Severity | Count | Findings |
| --- | --- | --- |
| Critical | 0 | — |
| High | 4 | #1 (package layout drift), #2 (ATR reimplemented inline), #3 (ATR not Wilder-smoothed), #4 (realized vol unit + Bessel) |
| Medium | 7 | #5 (silent provider failures), #6 (`Instant.now()` in calculator), #7 (no Lombok service style), #8 (magic thresholds in featurecalc), #11 (retry strategy + selection), #14 (test gaps), #15 (no session VWAP) |
| Low | 4 | #9, #10, #12, #13 |
| **Total** | **15** | |

The biggest correctness risk is **Findings #2/#3/#4 stacked**: feature math drifts from industry-standard definitions in three places, in ways that downstream scorers and calibration may have been silently tuned around. Either the documentation needs to say "this is our internal ATR variant, not Wilder's" (and similar for vol) and tests need to lock the definition; or the math needs to match the names. Without one of those, future contributors will either re-derive these on the assumption they're conventional or paper over them with more downstream magic numbers.
