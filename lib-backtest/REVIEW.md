# lib-backtest — Implementation Review

**Date:** 2026-05-17
**Last fix pass:** 2026-05-20 (local fixes only — threshold deduplication still open)
**Reviewer:** Automated audit pass
**Scope:** `lib-backtest/` module — 4 record models + 1 service class + 1 test class.

---

## Fix Status

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | High | Threshold duplication with production orchestrator | Open — needs shared `CandidateValidationInput` factory; cross-module refactor |
| 2 | Medium | `BacktestReplayEngine` not using Lombok service style | **Fixed** — `@RequiredArgsConstructor` replaces hand-written constructor |
| 3 | Medium | 3 of 5 README testing expectations uncovered | Open — additive test work |
| 4 | Medium | Locale-dependent `String.format` in `BacktestReport.summary` | **Fixed** — `Locale.ROOT` pinned with javadoc explaining the rationale |
| 5 | Medium | Positional 16-arg `CandidateValidationInput` construction | Open — needs migration to the `@Builder` added in lib-domain |
| 6 | Low | Mutable `EnumMap` exposed via `BacktestMetrics` | **Fixed** — compact constructor wraps in `Collections.unmodifiableMap(new EnumMap<>(...))` |
| 7 | Low | `BacktestOutcome` accepts null fields | **Fixed** — compact constructor `Objects.requireNonNull`s every field |
| 8 | Low | No `package-info.java` anywhere | **Fixed** — added to `dev.reddragon.backtest`, `.models`, `.services` |
| 9 | Low | `BacktestReport.summary` dead null-metrics branch | **Fixed** — compact constructor now `Objects.requireNonNull`s `strategyName` and `metrics`; `passRate()` and `summary()` cleaned up to assume non-null metrics |
| 10 | Low | `BacktestReplayEngine` Spring bean uncertainty | Open — verification of `app` wiring, not a code change in this module |
| 11 | Low | No null-strategy guard on `process()` | **Fixed** — `Objects.requireNonNull(strategyName, ...)` at top of `process` |

**Open: 5** — #1 is the headline architectural one (threshold dedup). The rest are additive (tests, builder migration, app-side verification) or intentionally deferred (#9 dead branch).

---

---

## Expected Behavior (per docs)

`lib-backtest/README.md` is the spec. Key claims:

- Deterministic replay harness; "identical inputs must produce identical outputs" (line 20).
- Supports L8 (Meta-System Adaptation) per the parent `ARCHITECTURE.md` mapping.
- Data flow (README lines 34–41): `List<BacktestFrame>` → `MarketFeatureCalculator.process` → `DeterministicAnalyticsService.process` → `DisequilibriumValidationEngine.process` → `BacktestOutcome` → `BacktestReport`.
- "Use the same engine as production" — design guidance (README lines 70–73): *"The backtest intentionally uses MarketFeatureCalculator, DeterministicAnalyticsService, and DisequilibriumValidationEngine without any special-casing."*
- Non-responsibilities (README lines 24–30): does not fetch live data, persist results, simulate fills/slippage, produce forward-looking estimates.
- `BacktestReplayEngine` is described as "Stateless service" (line 51, line 64).
- Testing expectations (README lines 80–88): empty list, single-frame matches direct engine call, verdict-distribution counts accurate, average-score matches manual calc, deterministic across repeated runs.

Project-wide conventions from `../ARCHITECTURE.md`:

- `@Value + @Accessors(fluent = true)` for value objects, `@RequiredArgsConstructor` for service constructors, `@Slf4j` for loggers.
- "No nested classes."
- `@Value` constructors must validate normalized ranges.

---

## Files Reviewed

| File | Purpose |
| --- | --- |
| `C:\repos\red-dragon\red-dragon\lib-backtest\pom.xml` | Maven descriptor; depends on lib-domain, lib-marketdata, lib-analytics, lib-validation; lombok + junit only. |
| `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestFrame.java` | Record: candidate + bars; defensive copy of bars. |
| `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestOutcome.java` | Record: candidate + marketData + analytics + validation. |
| `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestMetrics.java` | Record: totalFrames + averageScore + verdictCounts; static factory and rate helpers. |
| `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestReport.java` | Record: strategyName + metrics + outcomes; defensive copy + summary string. |
| `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\services\BacktestReplayEngine.java` | Service: wires the three pipeline stages and builds outcomes. |
| `C:\repos\red-dragon\red-dragon\lib-backtest\src\test\java\dev\reddragon\backtest\services\BacktestReplayEngineTest.java` | Four smoke tests. |

(No `package-info.java` files exist anywhere in lib-backtest — flagged below.)

---

## Findings

### 1. Backtest engine duplicates production threshold logic verbatim — "no special-casing" design guidance violated

**Severity:** High
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\services\BacktestReplayEngine.java:52-75`
- `C:\repos\red-dragon\red-dragon\app\src\main\java\dev\reddragon\app\services\pipeline\CandidatePipelineOrchestrator.java:111-113`

**Expected vs Actual:** README lines 70–73:

> The backtest intentionally uses `MarketFeatureCalculator`, `DeterministicAnalyticsService`, and `DisequilibriumValidationEngine` without any special-casing. If the production engine changes, backtest behavior changes with it — that is a feature, not a bug.

Actual: `BacktestReplayEngine.validationInput(...)` hard-codes the same risk-flag thresholds that `CandidatePipelineOrchestrator` hard-codes, duplicating them. From `BacktestReplayEngine.java:70-72`:

```java
candidate.earlynessScore() < 0.45,
marketData.liquidityScore() < 0.35 || marketData.volatilityStabilityScore() < 0.35,
marketData.rangePosition() > 0.90,
```

And from `CandidatePipelineOrchestrator.java:111-113`:

```java
candidate.earlynessScore() < 0.45,
marketData.liquidityScore() < 0.35 || marketData.volatilityStabilityScore() < 0.35,
marketData.rangePosition() > 0.90,
```

Two copies of the same three literal thresholds. The literal `0.45` and `0.35` `0.35` `0.90` magic numbers are the *opposite* of "no special-casing": they are special cases that have to be kept in sync.

**Why it matters:** This is the headline correctness risk for the whole module. The L8 calibration loop (per README and parent `ARCHITECTURE.md`) compares backtest verdicts to live verdicts as the basis for whether the model is drifting. If a future PR adjusts the production thresholds (say, the `< 0.45` earlyness gate becomes `< 0.50`) and the engineer forgets the backtest copy, calibration will silently report drift that is actually a thresholds-out-of-sync bug. There is no test, type, or comment marking these as a paired-edit.

**Proposed fix:** Either (a) extract the entire `validationInput` construction into a shared helper in `lib-validation` or `lib-domain` that both `CandidatePipelineOrchestrator` and `BacktestReplayEngine` call, or (b) move the threshold constants onto `ValidationThresholds` (where the rest of the validation thresholds already live) so a config change propagates to both call sites. Option (a) is preferable because it captures the *entire* `CandidateValidationInput` shape rather than just the thresholds.

---

### 2. `BacktestReplayEngine` does not follow the project's Lombok service-constructor convention

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\services\BacktestReplayEngine.java:21-35`

**Expected vs Actual:** `../ARCHITECTURE.md` lines 130–131:

> **Lombok is the default** for value objects (`@Value` + `@Accessors(fluent = true)`), service constructors (`@RequiredArgsConstructor`), and loggers (`@Slf4j`).

Actual: a hand-written constructor:

```java
public class BacktestReplayEngine {

    private final MarketFeatureCalculator marketFeatureCalculator;
    private final DeterministicAnalyticsService analyticsService;
    private final DisequilibriumValidationEngine validationEngine;

    public BacktestReplayEngine(
            MarketFeatureCalculator marketFeatureCalculator,
            DeterministicAnalyticsService analyticsService,
            DisequilibriumValidationEngine validationEngine
    ) {
        this.marketFeatureCalculator = marketFeatureCalculator;
        this.analyticsService = analyticsService;
        this.validationEngine = validationEngine;
    }
```

`@RequiredArgsConstructor` would replace lines 27–35 with one annotation. The module's pom already declares Lombok as a provided dependency.

**Why it matters:** The convention is documented as "the default." A new service that doesn't follow it sets a precedent and adds 9 lines of boilerplate that the rest of the codebase has erased. It is also noticeable in code review that this is the *only* lib-backtest class touching Lombok policy and it doesn't use Lombok at all.

**Proposed fix:** Annotate the class with `@RequiredArgsConstructor` and delete the explicit constructor. Confirm no Spring/CDI tooling complains.

---

### 3. Three of five README "Testing expectations" are uncovered

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-backtest\src\test\java\dev\reddragon\backtest\services\BacktestReplayEngineTest.java`

**Expected vs Actual:** README lines 80–88:

> Tests should cover:
> - empty frame list produces an empty report with zero metrics ✓
> - single frame: outcome matches a direct engine call with the same inputs ✗
> - verdict distribution counts are accurate ✗
> - average score matches manual calculation ✗
> - deterministic: same frames in same order produce the same report every time ✗

Only the empty-list case (and a thin "all four pipeline stages produced non-null output" smoke test) is implemented. The verdict-distribution, average-score, and determinism cases — the ones that actually protect the calibration loop — have no tests:

```java
@Test
void singleFrameRunsAllThreePipelineStagesAndYieldsOneOutcome() {
    BacktestFrame frame = new BacktestFrame(sampleCandidate(), sampleBars(20));
    BacktestReport report = engine.process("smoke", List.of(frame));

    assertEquals(1, report.outcomes().size());
    BacktestOutcome outcome = report.outcomes().get(0);
    assertSame(frame.candidate(), outcome.candidate(), "candidate is passed through unchanged");
    assertNotNull(outcome.marketData(), "market-feature stage ran");
    assertNotNull(outcome.analytics(),  "analytics stage ran");
    assertNotNull(outcome.validation(), "validation stage ran");
    assertEquals(1, report.metrics().totalFrames());
}
```

This test passes whether the pipeline produces correct results or just any non-null result. It cannot catch the Finding #1 drift risk.

**Why it matters:** Determinism (point 5) is the central claim of the module per README line 20 ("identical inputs must produce identical outputs"); it is the *only* property that justifies trusting backtest output. There is no test asserting it. A reasonable implementation would run the same fixture twice and `assertEquals(reportA, reportB)`, but `BacktestReport` and friends inherit `equals` from `record` so the assertion would work cleanly.

**Proposed fix:** Add four tests matching README lines 82–88. Specifically:
- `singleFrameMatchesDirectEngineCall` — manually invoke the three stages with the same inputs and compare the `BacktestOutcome` to the engine result.
- `verdictDistributionMatchesManualCount` — feed three frames known to produce PASS/WATCH/REJECT and assert the `EnumMap` counts.
- `averageScoreMatchesManualMean` — feed three known frames, manually compute mean validation score, assert equality with delta.
- `deterministicAcrossRepeatedRuns` — run `engine.process("x", frames)` twice, assert `reportA.equals(reportB)`.

---

### 4. `BacktestReport.summary()` uses `String.format` without an explicit `Locale`

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestReport.java:28-39`

**Expected vs Actual:** README line 26 shows the example output:

> `"DisequilibriumV1: 120 frames | pass=42% watch=28% reject=30% | avg score=0.71"`

That format requires a `.` decimal separator. Actual:

```java
return String.format("%s: %d frames | pass=%.0f%% watch=%.0f%% reject=%.0f%% | avg score=%.2f",
        strategyName,
        metrics.totalFrames(),
        metrics.passRate() * 100,
        metrics.watchRate() * 100,
        metrics.rejectRate() * 100,
        metrics.averageScore());
```

`String.format(format, args)` uses the default `Locale.getDefault(Locale.Category.FORMAT)`. On a JVM with a comma-decimal locale (de-DE, fr-FR, it-IT), `%.2f` of `0.71` becomes `"0,71"`, not `"0.71"`. The summary string is logged and surfaced through HTTP review endpoints, so the format must be stable across deployments.

**Why it matters:** Production may be deployed in a non-`en-US` locale (Docker base images frequently ship with `C.UTF-8` which is `.`-decimal, but Java still picks `en_US` or `POSIX` depending on which is installed). The bug is latent until someone runs the app on a workstation set to a comma-decimal locale, at which point the example in the README diverges from real output and any downstream parsing breaks.

**Proposed fix:** Use `String.format(Locale.ROOT, ...)` or `String.format(Locale.US, ...)`. Same fix should be applied to any other formatted output in this module, but there is only one site.

---

### 5. `validationInput(...)` hard-couples to record-positional construction of `CandidateValidationInput`

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\services\BacktestReplayEngine.java:52-75`

**Expected vs Actual:** The 16-argument `new CandidateValidationInput(...)` call relies entirely on argument position:

```java
return new CandidateValidationInput(
        candidate.candidateId(),
        candidate.symbol(),
        candidate.structuralRealityScore(),
        candidate.materialSignificanceScore(),
        candidate.earlynessScore(),
        analytics.equilibriumQualityScore(),
        analytics.reflexivityPotentialScore(),
        analytics.asymmetryScore(),
        analytics.regimeCompatibilityScore(),
        analytics.deploymentConfidenceScore(),
        candidate.hasCredibleStructuralCatalyst(),
        marketData.complete(),
        candidate.earlynessScore() < 0.45,
        marketData.liquidityScore() < 0.35 || marketData.volatilityStabilityScore() < 0.35,
        marketData.rangePosition() > 0.90,
        candidate.summary()
);
```

Five of these are doubles between `0.0` and `1.0` (`structuralRealityScore`, `materialSignificanceScore`, `earlynessScore`, `equilibriumQualityScore`, `reflexivityPotentialScore`). Three more are doubles representing different concepts. A reorder of two same-typed fields in `CandidateValidationInput` (e.g., swapping the analytics-score block) would compile silently and produce subtly wrong verdicts in backtest.

**Why it matters:** Records compose well with builders. If `CandidateValidationInput` had a Lombok-style builder (or static `of(...)` factory with named parameters), this fragility goes away. Today it is a single PR away from a deterministic-but-wrong backtest.

**Proposed fix:** Give `CandidateValidationInput` a Lombok builder (`@Builder` on the record) or a named-parameter factory, and switch this call to use it. Add a regression test that constructs a known input and asserts the field bindings.

---

### 6. `BacktestMetrics.from(...)` violates the no-nested-class convention indirectly via the static factory placement, and the mutable `EnumMap` it returns is exposed unwrapped

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestMetrics.java:14-28`

**Expected vs Actual:** ARCHITECTURE.md says shared cross-module foundations stay small and value objects use `@Value`. `BacktestMetrics` is a record, fine. But `from(...)` returns the record holding a *mutable* `EnumMap<Verdict, Long>`:

```java
Map<Verdict, Long> counts = new EnumMap<>(Verdict.class);
for (BacktestOutcome outcome : safeOutcomes) {
    Verdict verdict = outcome.validation().verdict();
    counts.put(verdict, counts.getOrDefault(verdict, 0L) + 1L);
}

return new BacktestMetrics(safeOutcomes.size(), averageScore, counts);
```

Records don't defensively copy mutable fields. A caller can downcast and mutate `verdictCounts`, which makes determinism (the headline claim) easily violable through side channels.

**Why it matters:** Low because no current caller mutates it, but the report is held by `BacktestReport` and may be serialized/cached in `app`. Once cached, any reader could perturb the cached counts.

**Proposed fix:** In the compact constructor, wrap with `Map.copyOf(verdictCounts)` or `Collections.unmodifiableMap(new EnumMap<>(verdictCounts))`. `BacktestFrame` and `BacktestReport` already follow this defensive-copy pattern for their list fields; `BacktestMetrics` should match.

---

### 7. `BacktestFrame` accepts a `null` bars list without flagging it; `BacktestOutcome` has no null-guarding at all

**Severity:** Low
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestFrame.java:11-18`
- `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestOutcome.java:8-14`

**Expected vs Actual:** ARCHITECTURE.md line 141: "`@Value` constructors do validation." `BacktestFrame` is a record (not `@Value`) but adopts the same intent with its compact constructor — it does `List.copyOf(bars == null ? List.of() : bars)` (line 16), which silently swaps a null for an empty list. `BacktestOutcome` does not validate at all:

```java
public record BacktestOutcome(
        TradeCandidate candidate,
        MarketDataSnapshot marketData,
        AnalyticsSnapshot analytics,
        ValidationResult validation
) {
}
```

`new BacktestOutcome(null, null, null, null)` is legal and silent.

**Why it matters:** A `BacktestOutcome` with `null candidate` will NPE the first time `report.outcomes().get(0).candidate().symbol()` is called by a UI or a calibration analyzer. Better to fail at construction.

**Proposed fix:** Add a compact constructor to `BacktestOutcome` that calls `Objects.requireNonNull(...)` on each field. For `BacktestFrame`, decide whether `null bars` is a valid "no bars available" sentinel — if yes, leave the current behavior and document it in javadoc; if no, throw.

---

### 8. No `package-info.java` files anywhere in `lib-backtest`

**Severity:** Low
**Files:** entire module.

**Expected vs Actual:** Parent ARCHITECTURE.md line 124–125:

> Every public package has a `package-info.java` calling out its MD layer.
> When in doubt, open that file first.

Actual: `find lib-backtest -name package-info.java` returns nothing. Compare to `lib-analytics`, which has `package-info.java` in every sub-package.

**Why it matters:** Onboarding clue. README line 3–5 already covers the MD-layer mapping but per the convention, the file should also exist in `dev.reddragon.backtest`, `dev.reddragon.backtest.models`, and `dev.reddragon.backtest.services`.

**Proposed fix:** Add three one-line `package-info.java` files. Each can be three lines naming the MD layer and pointing at README.md.

---

### 9. `BacktestReport.summary()` silently swallows `null metrics`

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\models\BacktestReport.java:28-39`

**Expected vs Actual:**

```java
public String summary() {
    if (metrics == null) {
        return strategyName + ": no metrics";
    }
    ...
}
```

`BacktestReport` is constructed only by `BacktestReplayEngine.process(...)` (line 41), which always passes a non-null `BacktestMetrics.from(outcomes)`. The null guard is dead code, and "no metrics" is a misleading sentinel — it implies an unexpected runtime path that cannot actually occur.

**Why it matters:** Dead error paths cause cargo-cult copy/paste in tests and downstream code. A future caller may assume `summary()` returns "no metrics" in some edge case and write code expecting it; that code will never trigger.

**Proposed fix:** Either remove the guard (rely on `Objects.requireNonNull` in a compact constructor — see Finding #7), or document the case as `@throws NullPointerException` and let it fail loud. Same for `BacktestReport.passRate()` (line 18–20).

---

### 10. `BacktestReplayEngine` is constructed by raw `new` in tests, not via Spring; the README claim that it is a "service" is therefore informal

**Severity:** Low
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\services\BacktestReplayEngine.java:21`
- `C:\repos\red-dragon\red-dragon\lib-backtest\src\test\java\dev\reddragon\backtest\services\BacktestReplayEngineTest.java:35-39`

**Expected vs Actual:** README lines 50–51 list `BacktestReplayEngine` as "Stateless service that maps frames to outcomes." There is no Spring stereotype on the class (no `@Service`, `@Component`, `@Bean`, etc.). lib-backtest's pom does not depend on `spring-context`, which is intentional and good. But that means a bean must be declared elsewhere (presumably in `app/.../PipelineConfiguration.java`) — otherwise the `@RestController` for `/api/backtest` cannot inject it.

**Why it matters:** Not a bug if `app/services/.../PipelineConfiguration.java` defines a `@Bean BacktestReplayEngine`. Worth verifying. The audit cannot confirm without reading `app`, but the test uses raw `new BacktestReplayEngine(...)` (line 35), which works regardless.

**Proposed fix:** Verify a `@Bean` declaration exists in `app`; if not, add one. If it does, no action needed.

---

### 11. No `BacktestReplayEngine.process(strategyName, ...)` null-strategy guard

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-backtest\src\main\java\dev\reddragon\backtest\services\BacktestReplayEngine.java:37-42`

**Expected vs Actual:** `process(String strategyName, List<BacktestFrame> frames)` accepts a null `strategyName` and propagates it into `BacktestReport`. README example output (line 26) implies the strategy name is part of the rendered identity (`"DisequilibriumV1: ..."`). A `null` strategy name yields `"null: ..."` in `summary()`.

**Why it matters:** Logged and surfaced via `GET /api/backtest/runs`. A run with a null strategy name will be hard to attribute later.

**Proposed fix:** `Objects.requireNonNull(strategyName, "strategyName")` at the top of `process`. Or accept the value and replace `null` with a documented sentinel like `"unnamed"`.

---

## Determinism Audit

Sources of nondeterminism examined:

- **Clock:** `BacktestReplayEngine` does not call `Instant.now()`, `System.currentTimeMillis()`, `LocalDate.now()`, etc. The pipeline stages it delegates to are deterministic by their own contracts (per `lib-analytics` README: "Pure functions in lib-analytics. No I/O, no static state, no portfolio."). ✓
- **Iteration order:** `frames.stream().map(...).toList()` preserves input order. ✓
- **Random:** No usage. ✓
- **Map iteration:** `BacktestMetrics.from` populates an `EnumMap`, which has stable iteration order. ✓
- **Concurrent collection ordering:** none used. ✓
- **Hidden state in dependencies:** `MarketFeatureCalculator`, `DeterministicAnalyticsService`, `DisequilibriumValidationEngine` are constructed once and reused. If any of them holds mutable state in violation of its own contract, that would leak in here. The test injects fresh instances per test class, so cross-test contamination is bounded.

No nondeterminism in the engine itself was found. The Finding #3 absence of a determinism test is what makes this assertion non-binding.

---

## Doc/Code Drift

| Documented in README | Implemented? | Notes |
| --- | --- | --- |
| `BacktestReplayEngine.process(strategyName, frames)` | ✓ | Matches exactly. |
| Three pipeline stages invoked, no special-casing | Partial | Stages are invoked correctly, but threshold logic for `CandidateValidationInput` is duplicated from `app` — see Finding #1. |
| `BacktestFrame` defensive copies bars | ✓ | Line 16. |
| `BacktestReport` defensive copies outcomes | ✓ | Line 11. |
| `BacktestMetrics.passRate / watchRate / rejectRate` | ✓ | All three present; behave correctly. |
| `summary()` example output | Locale-dependent | See Finding #4. |
| HTTP endpoints `POST /api/backtest`, `GET /api/backtest/results/{runId}`, `GET /api/backtest/runs` | Not in lib | All three live in `app/.../BacktestController` (out of scope here). |
| Five testing expectations | 1 of 5 covered | See Finding #3. |

---

## Strengths

1. **Module shape is correct.** Three pipeline stages threaded together exactly as documented; no extra side responsibilities. Persistence, fixture loading, and HTTP are correctly left to `app`.
2. **Records are used idiomatically.** Compact constructors do defensive copies of input lists on `BacktestFrame` and `BacktestReport`. Equality is structural for free (good for the determinism contract once tested — Finding #3).
3. **Engine has no mutable state.** Three `final` fields injected via constructor; no instance variables added per call. Concurrent replay is safe.
4. **Pom dependencies are tight.** Pulls only `lib-domain`, `lib-marketdata`, `lib-analytics`, `lib-validation`. No Spring runtime. No persistence. The library is a pure jar, exactly as the README demands.
5. **No persistence leakage.** The engine returns values; `app` is responsible for storing them. README non-responsibility ("Persist results directly") is honored.
6. **`BacktestMetrics.from(...)` correctly uses `EnumMap`** rather than `HashMap`, which is both faster and produces stable iteration order — important for deterministic equality of `BacktestReport`.
7. **Test fixture is realistic.** `sampleBars(20)` produces 20 monotonically rising bars with realistic OHLC relationships, giving the analytics and validation stages enough data to exercise their windowed math.
8. **No `Thread.sleep`, no executor pools, no async.** Replay is straight-line, which makes it both deterministic and trivially debuggable.

---

## Summary table — issue count by severity

| Severity | Count | Findings |
| --- | --- | --- |
| Critical | 0 | — |
| High | 1 | #1 (threshold duplication with production orchestrator) |
| Medium | 4 | #2 (Lombok convention), #3 (testing-expectations gap), #4 (locale-dependent format), #5 (positional record construction) |
| Low | 6 | #6, #7, #8, #9, #10, #11 |
| **Total** | **11** | |

The single biggest risk is **Finding #1**: the design guidance "no special-casing" is the module's reason for existing — calibration is only meaningful if backtest decisions are produced by the same code path as production. Today the two paths differ by three magic-number literals that are easy to update in one place and forget in the other. Closing that, plus adding the determinism test from Finding #3, removes the structural risk of the module.
