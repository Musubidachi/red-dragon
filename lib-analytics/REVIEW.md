# lib-analytics — Implementation Review

**Date:** 2026-05-17
**Last fix pass:** 2026-05-21 (Option A executed — scorers become the source of truth; orchestrator now delegates to all documented L3/L4/L5/L6 scorers). Earlier 2026-05-20 pass covered the local fixes.
**Reviewer:** Automated audit pass
**Scope:** `lib-analytics/` — ~30 source files spanning 6 MD-layer sub-packages plus the cross-layer orchestrators. This is the largest and most architecturally load-bearing module in the repo.

---

## Fix Status

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | Critical | Orchestrator reimplements scorers inline; standalone scorer classes are dead code | **Fixed** — Option A executed. `DeterministicAnalyticsService` now delegates to `RegimeCompatibilityScorer`, `EquilibriumQualityScorer`, `AsymmetryScorer`, `ReflexivityScorer`, `DeploymentConfidenceScorer`, `AdversarialValidationAnalyzer`, and `PropagationPhaseAnalyzer`. Inline math deleted; the 28 constants previously embedded in the orchestrator moved into the scorer classes. **Note: this is a real math change.** Equilibrium quality went from 2-input average to 3-input weighted (+rangeBalance); reflexivity from 50/50 to 60/40; deployment confidence from 5 to 7 inputs with rebalanced weights. Score drift documented in lib-analytics/REVIEW.md before the fix; calibration history pre-2026-05-21 reflects the older math. |
| 2 | Critical | Orchestrator and standalone classifier disagree on regime label/score | **Fixed** — `RegimeCompatibilityScorer.process` now classifies all 7 `RegimeLabel` values (added `HOSTILE_NEWS_DRIVEN` and `SUPPORTIVE_COMPRESSION` branches matching the orchestrator's previous inline logic). Both code paths produce identical labels for identical inputs. |
| 3 | High | Scorers mutate `List<String> notes` parameters | Open — fix requires changing public scorer signatures; large blast radius |
| 4 | High | `MarketStateClassifier` user-visible disagreement | **Fixed** — corollary of #2; standalone classifier and pipeline produce the same labels now |
| 5 | Medium | `Instant.now()` in `buildSnapshot` | **Fixed** — uses `marketData.observedAt()` for deterministic backtests |
| 6 | Medium | `new PropagationPhaseAnalyzer()` not injected | **Fixed** — every scorer now passed via constructor; `@RequiredArgsConstructor`-style hand-written constructor with `Objects.requireNonNull` on each |
| 7 | Medium | Pointless ternary in `propagationPhase` | **Fixed** — collapsed to `slope * 0.5` with clarifying comment |
| 8 | Medium | Locale-dependent `String.format` | **Fixed** — `EquilibriumCompressionScorer.format` uses `Locale.ROOT` |
| 9 | Medium | `default -> 0.00` in RegimeCompatibilityScorer.score | **Fixed** — exhaustive switch, future enum additions fail compile |
| 10 | Medium | Hardcoded calibration thresholds in `LongHorizonCalibrationAnalyzer` | **Fixed** — extracted to named static finals; cross-module move to `ValidationThresholds` still tracked |
| 11 | Medium | `adversarialNotes` duplicates `AdversarialValidationAnalyzer` | **Partially fixed** — orchestrator now calls the full `AdversarialValidationAnalyzer` (which gained null-tolerance for the snapshots the pipeline doesn't have) AND keeps three lightweight checks for the candidate-only / pipeline-only flags that use orchestrator-derived `reflexivity` and `MarketDataSnapshot.liquidityScore/volatilityStabilityScore` rather than the richer snapshot types. The two paths are clearly named and isolated. |
| 12 | Low | Hardcoded orchestrator thresholds | **Fixed** — was already done; thresholds now live in the scorers (orchestrator only keeps three `LITE_*` constants for the lightweight adversarial checks) |
| 13 | Medium | Tests at scorer boundary; don't catch orchestrator drift | Open — but materially less risky now that the orchestrator IS calling the scorers; boundary tests now exercise production code paths |
| 14 | Low | L7 doesn't follow L5 split pattern | **Fixed (documented)** — added a "Note on L5-style split" block to ARCHITECTURE.md L7 section explaining that the exit scorer maps directly to a recommendation enum because the output is advisory-only |

**Open: 2** — #3 (notes-list mutation pattern across all scorers; large signature change) and #13 (additive integration test work). Both are now low-risk follow-ups, not architectural concerns.

## What changed in the Option A pass

- **`RegimeCompatibilityScorer.process`** now has 7 classification branches (was 5). The two new branches — `HOSTILE_NEWS_DRIVEN` and `SUPPORTIVE_COMPRESSION` — use the same thresholds the orchestrator was using inline.
- **`AdversarialValidationAnalyzer.process`** is now null-tolerant: callers can pass `null` for any of the six optional snapshots and the affected checks silently skip. The required parameters are `TradeCandidate` and `MarketDataSnapshot`.
- **`DeterministicAnalyticsService`** has two constructors:
    - A no-arg convenience constructor (existing wiring + tests still compile) that news-up default instances.
    - An all-args constructor that takes 7 scorer dependencies and `requireNonNull`s each.
- **Score math is now slightly different** because the scorers use more sophisticated formulas than the inline code did. The relevant table from the pre-fix REVIEW.md (under "Concrete impact" in the original audit):

| Score | Inline (before fix) | Scorer-based (after fix) |
|---|---|---|
| equilibriumQuality | `average(liquidity, volStab)` | `liquidity*0.35 + volStab*0.40 + rangeBalance*0.25` |
| reflexivity | `average(reflexivityPotential, earlyness)` | `reflexivityPotential*0.60 + earlyness*0.40` |
| deploymentConfidence | 5 inputs, weighted | 7 inputs, rebalanced weights (includes equilibriumQuality + reflexivity) |
| asymmetry | same formula | same formula, but consumes the (now-different) equilibriumQuality |
| regimeLabel / regimeCompatibility | same | same (no math change; only the classifier's coverage expanded) |

Calibration data collected before the 2026-05-21 fix should be considered baseline-drifted under the new math. Re-calibration on outcomes collected after this date is recommended.

---

---

---

## Expected Behavior (per docs)

`lib-analytics/README.md` is the in-module spec:

- Pure deterministic scoring for the disequilibrium pipeline (line 3).
- No I/O, no persistence, no portfolio awareness (line 38–39).
- Same input → same output (line 40).
- Scores carry explanation data (line 41).
- No nested classes (line 42).
- New scorers belong in the layer package matching their job (line 43).

**MD-layer mapping (README lines 9–19):**

| Package | Layer | Job |
| --- | --- | --- |
| `services/structural/` | L3 | Catalyst reality, materiality, dilution risk, asymmetry, adversarial checks. |
| `services/classification/` | L4 | Regime compatibility, equilibrium quality, VWAP behavior, liquidity texture, volatility, options flow, trend persistence. |
| `services/deployment/` | L5 | Deployment confidence input. Final tiering lives in `lib-validation`. |
| `services/propagation/` | L6 | Narrative expansion, propagation phase, reflexivity, sector propagation. |
| `services/exit/` | L7 | Equilibrium compression and exit/tightening signal generation. |
| `services/meta/` | L8 | Live context adaptation and long-horizon calibration drift. |
| `services/` | cross-layer | Orchestrators (`DeterministicAnalyticsService`, `MarketStateClassifier`). |

**Public API (README lines 23–31):**

- `DeterministicAnalyticsService` — main pipeline entry; `TradeCandidate + MarketDataSnapshot → AnalyticsSnapshot`.
- `MarketStateClassifier` — exposed via `POST /api/market-state/classify`.
- `EquilibriumCompressionScorer` — exposed via `POST /api/exit-signal`.
- `LongHorizonCalibrationAnalyzer` — L8 drift analysis from realized outcomes.

`META_ADAPTATION_FEEDBACK.md` is an explicit forward-looking design doc (line 12: "This is not yet implemented"). Today's L8 surface is **only** what `LongHorizonCalibrationAnalyzer` produces. The rest of the document is future work and is **not** expected to be in the code.

Project-wide conventions:
- `@RequiredArgsConstructor`, `@Slf4j` for services.
- `@Value + @Accessors(fluent = true)` for value objects.

---

## Files Reviewed

| Sub-package | Files |
| --- | --- |
| `services/` (cross-layer) | `DeterministicAnalyticsService.java`, `MarketStateClassifier.java` |
| `services/structural/` (L3) | `AsymmetryScorer.java`, `DilutionRiskScorer.java`, `MaterialityImpactScorer.java`, `AdversarialValidationAnalyzer.java` |
| `services/classification/` (L4) | `RegimeCompatibilityScorer.java`, `EquilibriumQualityScorer.java`, `EquilibriumPhaseAnalyzer.java`, `DirectionalPersistenceScorer.java`, `VolatilityExpansionScorer.java`, `VwapInteractionScorer.java`, `LiquidityTextureScorer.java`, `OptionsFlowScorer.java` |
| `services/deployment/` (L5) | `DeploymentConfidenceScorer.java` |
| `services/propagation/` (L6) | `NarrativeExpansionScorer.java`, `PropagationPhaseAnalyzer.java`, `ReflexivityScorer.java`, `SectorPropagationScorer.java` |
| `services/exit/` (L7) | `EquilibriumCompressionScorer.java` |
| `services/meta/` (L8) | `LiveContextAdaptationAnalyzer.java`, `LongHorizonCalibrationAnalyzer.java` |
| Package-info | one per sub-package |
| Tests | 8 files (one boundary test per sub-package + orchestrator + classifier) |

---

## Findings

### 1. `DeterministicAnalyticsService` reimplements most documented scorers inline; the L3/L4/L5/L6 scorers it claims to orchestrate are effectively dead code from the orchestrator's perspective

**Severity:** Critical
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\DeterministicAnalyticsService.java:23-267`

**Expected vs Actual:** README line 25–26: *"`DeterministicAnalyticsService` — main pipeline entry. Takes a `TradeCandidate` and `MarketDataSnapshot`, returns an `AnalyticsSnapshot`."* `ARCHITECTURE.md` lines 121–122 list this as the orchestrator that "calls everything in turn." Actual: the orchestrator instantiates exactly one scorer (`PropagationPhaseAnalyzer`) and reimplements the rest inline.

```java
// DeterministicAnalyticsService.java:25
private final PropagationPhaseAnalyzer propagationPhaseAnalyzer = new PropagationPhaseAnalyzer();
```

That's the only scorer field. Then `process(...)` does:

```java
// DeterministicAnalyticsService.java:30-66
RegimeLabel regime = regime(marketData, notes);                            // inline copy of RegimeCompatibilityScorer
double regimeCompatibility = regimeCompatibility(regime);                  // inline copy of RegimeCompatibilityScorer.score
double equilibriumQuality = equilibriumQuality(marketData);                // inline copy of EquilibriumQualityScorer
double asymmetry = asymmetry(candidate, marketData, equilibriumQuality, notes);  // inline copy of AsymmetryScorer
double reflexivity = reflexivity(candidate);                               // inline copy of ReflexivityScorer
double deploymentConfidence = deploymentConfidence(...);                   // inline copy of DeploymentConfidenceScorer
...
adversarialNotes(candidate, marketData, reflexivity, notes);               // inline copy of AdversarialValidationAnalyzer (lite)
```

And the inline `asymmetry` method **is verbatim identical** to `AsymmetryScorer.process`:

```java
// DeterministicAnalyticsService.java:225-242 (inline)
private double asymmetry(...) {
    double rangePenalty = rangePenalty(marketData, notes);   // same 0.20 penalty threshold 0.85
    double gapPenalty = gapPenalty(marketData, notes);       // same 0.15 penalty threshold 0.12
    return AnalyticsScoreUtils.clamp(
            candidate.structuralRealityScore() * 0.25
                    + candidate.materialSignificanceScore() * 0.25
                    + candidate.earlynessScore() * 0.25
                    + equilibriumQuality * 0.25
                    - rangePenalty
                    - gapPenalty
    );
}
```

```java
// AsymmetryScorer.java:18-39 (sibling)
public double process(TradeCandidate candidate, MarketDataSnapshot marketData,
                      double equilibriumQuality, List<String> notes) {
    double rangePenalty = rangePenalty(marketData, notes);   // identical penalty
    double gapPenalty = gapPenalty(marketData, notes);
    return AnalyticsScoreUtils.clamp(
            candidate.structuralRealityScore() * 0.25
                    + candidate.materialSignificanceScore() * 0.25
                    + candidate.earlynessScore() * 0.25
                    + equilibriumQuality * 0.25
                    - rangePenalty
                    - gapPenalty
    );
}
```

Same for `regime()` (regimeCompatibility logic, lines 174–211) which is a near-duplicate of `RegimeCompatibilityScorer.process` (lines 17–46), with one critical divergence (Finding #2 below).

**Why it matters:** This is the single biggest source of architectural drift in the codebase. Three concrete consequences:

1. **The MD-layer mapping in README/ARCHITECTURE.md is misleading.** A reader concludes that "L3 asymmetry" lives in `AsymmetryScorer.java`. A code change there does not affect what runs in production. The active code path lives in `DeterministicAnalyticsService`.
2. **Junior-developer onboarding misleads.** ARCHITECTURE.md line 121 directs new developers to read `DeterministicAnalyticsService.java` as "the orchestrator that calls everything in turn." It calls almost nothing.
3. **Test coverage is at the wrong layer.** `StructuralScorersBoundaryTest`, `ClassificationScorersBoundaryTest`, etc., test the dead-code scorers; the production behavior in the orchestrator can drift undetected.

**Proposed fix:** Refactor `DeterministicAnalyticsService` to inject and call the documented scorers. Either:
- Lombok-`@RequiredArgsConstructor` with the L3/L4/L5/L6 scorers as constructor params, and let `app/.../PipelineConfiguration` wire them.
- Or, accept that the inline code is the production implementation and **delete the duplicate scorer classes** (or mark them deprecated with javadoc pointing at the inline equivalents). README and ARCHITECTURE.md should then be updated to remove the scorer references.

Either is a real fix. Today's middle ground (both exist, only one runs) is the worst option.

---

### 2. `RegimeCompatibilityScorer.process` and `DeterministicAnalyticsService.regime` produce DIFFERENT regime labels for the same input

**Severity:** Critical
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\classification\RegimeCompatibilityScorer.java:17-76`
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\DeterministicAnalyticsService.java:174-223`

**Expected vs Actual:** Per ARCHITECTURE.md L4: "Regime compatibility scorer" is a single source of truth. Actual: two divergent implementations that the orchestrator and the standalone classifier endpoint produce by different code paths.

Inline (`DeterministicAnalyticsService.regime`) returns 6 labels:
- `HOSTILE_NEWS_DRIVEN` (when `|gapPercent| > 0.15 && volStability < 0.50`)
- `HOSTILE_LIQUIDITY` (liquidityScore < 0.35)
- `HOSTILE_VOLATILITY` (volStability < 0.35)
- `SUPPORTIVE_COMPRESSION` (range 0.45..0.55, volStability ≥ 0.70, liquidity ≥ 0.60)
- `SUPPORTIVE_ROTATIONAL` (range 0.35..0.75)
- `SUPPORTIVE_TREND` (range > 0.75, volStability ≥ 0.60)
- `MIXED` (else)

Sibling (`RegimeCompatibilityScorer.process`) returns 5 labels:
- `HOSTILE_LIQUIDITY` (liquidityScore < 0.35)
- `HOSTILE_VOLATILITY` (volStability < 0.35)
- `SUPPORTIVE_ROTATIONAL` (range 0.35..0.75)
- `SUPPORTIVE_TREND` (range > 0.75, volStability ≥ 0.60)
- `MIXED` (else)

The sibling NEVER returns `HOSTILE_NEWS_DRIVEN` (no `gapPercent` check) and NEVER returns `SUPPORTIVE_COMPRESSION` (no compression branch). These are both real `RegimeLabel` enum values (`lib-domain/models/RegimeLabel.java:11-17`).

Then the score-mapping switches diverge worse:

```java
// DeterministicAnalyticsService.regimeCompatibility (line 213-223)
return switch (regimeLabel) {
    case SUPPORTIVE_ROTATIONAL -> 0.85;
    case SUPPORTIVE_TREND -> 0.70;
    case SUPPORTIVE_COMPRESSION -> 0.72;
    case MIXED -> 0.50;
    case HOSTILE_NEWS_DRIVEN -> 0.22;
    case HOSTILE_VOLATILITY -> 0.25;
    case HOSTILE_LIQUIDITY -> 0.20;
};
```

```java
// RegimeCompatibilityScorer.score (line 48-57)
return switch (regimeLabel) {
    case SUPPORTIVE_ROTATIONAL -> 0.85;
    case SUPPORTIVE_TREND -> 0.70;
    case MIXED -> 0.50;
    case HOSTILE_VOLATILITY -> 0.25;
    case HOSTILE_LIQUIDITY -> 0.20;
    default -> 0.00;
};
```

`SUPPORTIVE_COMPRESSION` and `HOSTILE_NEWS_DRIVEN` get `0.00` from the standalone scorer but `0.72` and `0.22` from the orchestrator.

**Why it matters:** `POST /api/market-state/classify` invokes the standalone classifier; the full pipeline invokes the orchestrator. A SUPPORTIVE_COMPRESSION input passes through `/api/market-state/classify` and is mapped to `0.00` compatibility — making the entire input look hostile — while the same input through the main pipeline gets `0.72`. Same data, opposite verdict. This is a production correctness bug, not a documentation bug.

**Proposed fix:** Pick one canonical implementation. Add `HOSTILE_NEWS_DRIVEN` branch to `RegimeCompatibilityScorer.process` and `SUPPORTIVE_COMPRESSION` case to `.score`, with the same constants the orchestrator uses. Then have `DeterministicAnalyticsService` delegate to it. Add a switch-exhaustiveness test that every `RegimeLabel` value produces a non-default score.

---

### 3. Scorers mutate `List<String> notes` parameters — pure-function rule (README rule #1) is violated

**Severity:** High
**Files (selection):**
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\structural\AsymmetryScorer.java:18, 28, 29, 46, 58`
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\classification\RegimeCompatibilityScorer.java:17-46`
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\meta\LongHorizonCalibrationAnalyzer.java:30-49`
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\DeterministicAnalyticsService.java:174, 248, 261`

**Expected vs Actual:** README lines 36–43 — Rule 1: *"Pure functions only: no I/O, no persistence, no static mutable state, no portfolio awareness."* Rule 2: *"Same input means same output."* Actual: many scorers receive a `List<String> notes` parameter that they mutate as a side effect:

```java
// AsymmetryScorer.java:41-51
private double rangePenalty(MarketDataSnapshot marketData, List<String> notes) {
    if (marketData.rangePosition() > 0.85) {
        notes.add("Range position is extended; remaining asymmetry may be compressed.");
        return 0.20;
    }
    return 0.0;
}
```

Strictly, the function is deterministic (same input → same output + same mutation), so Rule 2 is preserved if you consider the list mutation as part of the output. But Rule 1 explicitly forbids mutating shared state, and `List<String>` is mutable shared state by definition. Worse, the same list is passed to many scorers in sequence, so the order and contents depend on call order — a subtle dependency that violates the spirit of pure functions.

**Why it matters:** Three concerns:
1. **Composability.** A test that wants to verify "what notes does `AsymmetryScorer` produce" must allocate a list, call the scorer, and inspect the list. There's no value-level handshake.
2. **Concurrency.** If two threads share a `notes` list (unlikely today, but ARCHITECTURE.md allows analytics to be invoked concurrently), `ArrayList` is not thread-safe.
3. **Debuggability.** A note showing up in the final snapshot has no provenance — which scorer added it? You have to grep the source.

**Proposed fix:** Have each scorer return a small record:

```java
record ScoredResult(double score, List<String> notes) { }
```

Caller composes lists at the end. `EquilibriumCompressionScorer` already follows this pattern (returns `ExitSignal` containing notes). Other scorers should match.

---

### 4. `MarketStateClassifier` and the orchestrator's `regime()` produce different labels for the same `MarketDataSnapshot`

**Severity:** High
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\MarketStateClassifier.java` (entry to `RegimeCompatibilityScorer`)
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\DeterministicAnalyticsService.java:174-211`

**Expected vs Actual:** README line 27: *"`MarketStateClassifier` — standalone regime classifier exposed through `POST /api/market-state/classify`."* The standalone classifier and the inline orchestrator regime function must agree for the API to be trustworthy. They do not — see Finding #2. This is the same root cause but worth calling out as a user-visible bug: `POST /api/market-state/classify` will, for the same snapshot, return a different regime than appears on the trader review surface for a candidate using that snapshot.

**Why it matters:** Trader confusion + audit confusion. A "Hostile (News)" classification on one screen and "Mixed" on another for the same observation is a credibility issue with the platform.

**Proposed fix:** Merge with Finding #1/#2 fix.

---

### 5. `DeterministicAnalyticsService.process` calls `Instant.now()` to stamp `AnalyticsSnapshot.observedAt`

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\DeterministicAnalyticsService.java:163`

**Expected vs Actual:** README rule #2: "Same input means same output." Actual:

```java
return new AnalyticsSnapshot(
        candidate.candidateId(),
        candidate.symbol(),
        Instant.now(),
        regime,
        ...
);
```

`Instant.now()` is wall-clock dependent. Two calls with the same `(TradeCandidate, MarketDataSnapshot)` produce two non-equal `AnalyticsSnapshot.observedAt`. This is the same issue lib-domain Finding #5 and lib-marketdata Finding #6 raised at the producer side; it appears again in the orchestrator.

**Why it matters:** Same backtest determinism break and equals/hashCode time dependency.

**Proposed fix:** Inject a `Clock`. Or derive `observedAt` from the marketData snapshot's `observedAt`.

---

### 6. `propagationPhaseAnalyzer` is instantiated as a `new` field, not injected

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\DeterministicAnalyticsService.java:25`

**Expected vs Actual:**

```java
private final PropagationPhaseAnalyzer propagationPhaseAnalyzer = new PropagationPhaseAnalyzer();
```

Hard-coupled. Not a Lombok `@RequiredArgsConstructor`. Inconsistent with the rest of the codebase, which threads service dependencies through Spring DI.

**Why it matters:** Cannot be mocked or swapped in tests; cannot be configured per profile. Sets a precedent for the next contributor adding a new scorer to also `new` it inline.

**Proposed fix:** Convert to constructor injection. (Same fix is required by Finding #1 anyway.)

---

### 7. `DeterministicAnalyticsService.propagationPhase` "simplified second derivative" branches both compute the same value

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\DeterministicAnalyticsService.java:73-80`

**Expected vs Actual:**

```java
double acceleration = slope > 0 ? slope * 0.5 : slope * 0.5; // simplified second derivative
```

The conditional is pointless — both branches return `slope * 0.5`. The comment ("simplified second derivative") suggests the author intended different scaling for positive and negative slopes (deceleration weighted differently from acceleration), but the code does not implement that.

**Why it matters:** The propagation phase result depends on `acceleration`. If the intent was an asymmetric scaling (e.g., `slope * 0.5` vs `slope * 1.0`), the produced phase will be miscategorized. If the intent was the literal value `slope * 0.5`, the ternary should be removed and replaced with a single multiplication.

**Proposed fix:** Replace with `double acceleration = slope * 0.5;` and remove the misleading comment. Or, if the intent was asymmetric, restore the intended logic and document it.

---

### 8. `EquilibriumCompressionScorer.format` uses default-locale `String.format`

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\exit\EquilibriumCompressionScorer.java:174-176`

**Expected vs Actual:**

```java
private String format(double value) {
    return String.format("%.2f", value);
}
```

Used in the user-visible note `"Recommendation: " + recommendation.name() + " (compression=" + format(finalScore) + ")."` (line 58). In a comma-decimal locale, this produces `compression=0,71` instead of `compression=0.71`. Same root cause as lib-backtest Finding #4.

**Why it matters:** The exit endpoint (`POST /api/exit-signal`) returns this text in `ExitSignal.notes`. Locale drift across the API would produce inconsistent JSON output across environments.

**Proposed fix:** `String.format(Locale.ROOT, "%.2f", value)`.

---

### 9. `RegimeCompatibilityScorer.score` uses `default -> 0.00` instead of exhaustive case coverage

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\classification\RegimeCompatibilityScorer.java:48-57`

**Expected vs Actual:**

```java
public double score(RegimeLabel regimeLabel) {
    return switch (regimeLabel) {
        case SUPPORTIVE_ROTATIONAL -> 0.85;
        case SUPPORTIVE_TREND -> 0.70;
        case MIXED -> 0.50;
        case HOSTILE_VOLATILITY -> 0.25;
        case HOSTILE_LIQUIDITY -> 0.20;
        default -> 0.00;
    };
}
```

The `default -> 0.00` silently swallows `HOSTILE_NEWS_DRIVEN` and `SUPPORTIVE_COMPRESSION` (both real enum values per `lib-domain/models/RegimeLabel.java:11-17`). With a switch expression on an `enum`, omitting `default` makes the compiler enforce exhaustive coverage. Adding `default` defeats that safety net.

**Why it matters:** Future `RegimeLabel` additions will silently fall through to `0.00`. The drift in Finding #2 was enabled by this.

**Proposed fix:** Remove `default ->` and add explicit cases for `HOSTILE_NEWS_DRIVEN`, `SUPPORTIVE_COMPRESSION`.

---

### 10. `LongHorizonCalibrationAnalyzer.determineDrift` hardcodes calibration thresholds with no profile awareness

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\meta\LongHorizonCalibrationAnalyzer.java:84-115`

**Expected vs Actual:** The drift level depends on win rate, average return, and average drawdown thresholds:

```java
if (winRate >= 0.65 && averageReturn > 0.0 && averageDrawdown < 0.15) {
    ...
    return CalibrationDriftLevel.STABLE;
}
if (winRate >= 0.55 && averageReturn >= 0.0) {
    return CalibrationDriftLevel.MINOR_DRIFT;
}
if (winRate >= 0.45) {
    return CalibrationDriftLevel.MODERATE_DRIFT;
}
```

`META_ADAPTATION_FEEDBACK.md` §5 explicitly calls out that no threshold should be hardcoded outside the proposed-not-enforced governance system. The thresholds `0.65 / 0.55 / 0.45 / 0.15` are baked in. They're also not in `ValidationThresholds` in lib-validation.

**Why it matters:** As the L8 doc points out, market conditions evolve. A win-rate of 0.55 in 2026 may be "stable" relative to the bar's baseline; in 2024 it may have been "moderate drift." There is no way to tune this without a code change.

**Proposed fix:** Move the four thresholds into `ValidationThresholds` (or a new `CalibrationThresholds` config) and inject them. Document the default values per profile.

---

### 11. `DeterministicAnalyticsService.adversarialNotes` duplicates `AdversarialValidationAnalyzer` with weaker coverage

**Severity:** Medium
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\DeterministicAnalyticsService.java:87-105`
- `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\structural\AdversarialValidationAnalyzer.java:26-205`

**Expected vs Actual:** `AdversarialValidationAnalyzer` has 10 adversarial check functions (`hypeWithoutStructure`, `lateNarrativeAfterRepricing`, `optionsChaseWithoutReality`, `volatilityWithLiquidityDeterioration`, `materialityWithoutResponse`, `sectorSympathyWithoutCatalyst`, `strongCatalystIsolated`, `vwapReclaimWeakEquilibrium`, `highMentionLowCoherence`, `asymmetryCompressedByExtension`).

Inline `adversarialNotes` runs only 3 checks (hype-without-structure, late-entry, liquidity deterioration) — and the inline copies use slightly different thresholds (`structuralRealityScore() < 0.45` vs the standalone's `< 0.45` + extra propagation check).

The standalone analyzer is comment-documented as: *"Full adversarial analysis requiring intraday/options snapshots is out of scope for the standard pipeline run."* — so the inline lite version is intentional. But that intent should be made explicit: `AdversarialValidationAnalyzer` becomes a richer-input scorer the orchestrator doesn't use, while the orchestrator runs a stripped-down version. Both exist; only one runs.

**Why it matters:** Same architectural drift as Finding #1, scoped to adversarial checks. The 10-check analyzer reads as "production behavior" to a new dev; the 3-check inline list is what actually runs in the candidate pipeline.

**Proposed fix:** Either inject `AdversarialValidationAnalyzer` and pass the 8 snapshot params (most of which the orchestrator doesn't have — that's why the lite version exists), or document the lite version explicitly in javadoc with a pointer to the analyzer.

---

### 12. `DeterministicAnalyticsService.adversarialNotes`, `regime`, and `rangePenalty/gapPenalty` thresholds are not configurable

**Severity:** Low
**Files:** `DeterministicAnalyticsService.java` lines 94, 98, 102, 175, 180, 185, 190, 198, 203, 248, 260.

**Expected vs Actual:** 11 hardcoded `> 0.x` / `< 0.x` thresholds inside the orchestrator. All thresholds should live in `lib-validation/ValidationThresholds` (or a sibling `AnalyticsThresholds`) so a validation profile can adjust them per region/regime.

**Why it matters:** lib-backtest Finding #1 is the headline; this is the lib-analytics counterpart.

**Proposed fix:** Move every magic number to a `Map<String, Double>` config bean injected via constructor. Use named constants for documentation.

---

### 13. Tests are at scorer boundary level; they do not catch the orchestrator-bypasses-scorers drift

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\test\java\dev\reddragon\analytics\services\`

**Expected vs Actual:** Test files:

```
DeterministicAnalyticsServiceTest.java          -- orchestrator smoke
MarketStateClassifierTest.java                  -- standalone classifier
classification/ClassificationScorersBoundaryTest.java
deployment/DeploymentConfidenceScorerTest.java
exit/EquilibriumCompressionScorerTest.java
meta/LongHorizonCalibrationAnalyzerTest.java
propagation/PropagationScorersBoundaryTest.java
structural/StructuralScorersBoundaryTest.java
```

The boundary tests exercise the standalone scorers. The orchestrator test exercises `DeterministicAnalyticsService.process(...)` end-to-end. There is no test asserting "the orchestrator agrees with the standalone scorer for the same input" — and there cannot be, since they disagree (Findings #1, #2).

**Why it matters:** A green test suite today coexists with the bugs in Findings #1, #2, #4. Test coverage is at the wrong layer.

**Proposed fix:** After fixing Finding #1, replace the orchestrator's inline logic with calls to the scorers, then add an equivalence test: `assertEquals(orchestrator.process(...).regimeLabel(), classifier.process(...).regimeLabel())`.

---

### 14. The L7 `EquilibriumCompressionScorer` is the *only* scorer that does its own thresholding into a recommendation enum

**Severity:** Low (informational)
**File:** `C:\repos\red-dragon\red-dragon\lib-analytics\src\main\java\dev\reddragon\analytics\services\exit\EquilibriumCompressionScorer.java:167-172`

**Expected vs Actual:** Per ARCHITECTURE.md L5: "Deployment Engine — final tier decision lives in `lib-validation/services/engine/DeploymentResolver`." That convention treats lib-analytics as the *scoring* layer and lib-validation as the *decision* layer. But L7's scorer converts a `compression` score into an `ExitRecommendation` enum (HOLD/TIGHTEN/SCALE_OUT/EXIT_NOW) entirely inside lib-analytics:

```java
if (compression >= EXIT_THRESHOLD)      return ExitRecommendation.EXIT_NOW;
if (compression >= SCALE_OUT_THRESHOLD) return ExitRecommendation.SCALE_OUT;
if (compression >= TIGHTEN_THRESHOLD)   return ExitRecommendation.TIGHTEN;
return ExitRecommendation.HOLD;
```

There's no `ExitResolver` in lib-validation. This breaks the pattern documented for L5.

**Why it matters:** Either (a) the L5 pattern (split scorer vs resolver) is the canonical convention and L7 should follow, with the recommendation enum decision in lib-validation; or (b) L7 is intentionally simpler because exit is advisory-only and the trader makes the final call. Today the doc implies (a) but the code follows (b) without saying so.

**Proposed fix:** Add a one-line note to ARCHITECTURE.md (L7 section) clarifying that exit recommendation is determined in lib-analytics because the L5-style scorer/resolver split is unnecessary for advisory-only outputs.

---

### 15. `META_ADAPTATION_FEEDBACK.md` "What ships today" claims `LongHorizonCalibrationAnalyzer` outputs `CalibrationDriftLevel` (STABLE / MINOR / MODERATE / MAJOR). Verified.

**Severity:** N/A — confirmation
**File:** `lib-analytics/src/main/java/dev/reddragon/analytics/services/meta/LongHorizonCalibrationAnalyzer.java`

The forward-looking design in META_ADAPTATION_FEEDBACK.md (DimensionAttributionAnalyzer, StratifiedCalibrationAnalyzer, MarketStructureDriftMonitor, TraderOverrideAnalyzer, CounterfactualReplayAnalyzer, ThresholdChangeProposer) is **not** implemented — and the doc explicitly says it isn't (line 12: "This is not yet implemented"). The doc and code are aligned on what currently ships.

---

## Determinism / Purity Audit

| Rule (README §3) | Status | Notes |
| --- | --- | --- |
| No I/O | ✓ | No file/network calls in any reviewed scorer. |
| No persistence | ✓ | No JPA, no repositories. |
| No static mutable state | ✓ | No `static` mutable fields. Constants are `static final`. |
| No portfolio awareness | ✓ | No PnL, no position, no account. |
| Same input → same output | ✗ | `Instant.now()` in `DeterministicAnalyticsService.buildSnapshot` (Finding #5). |
| No nested classes | ✓ | Verified via Grep — no nested class declarations in any reviewed file. |

The single determinism violation is the `Instant.now()` call. Fixing Finding #5 closes the purity gap module-wide.

---

## Doc/Code Drift Summary

| README claim | Reality |
| --- | --- |
| `DeterministicAnalyticsService` is the main pipeline that calls L3–L6 scorers | False — orchestrator reimplements inline (Finding #1) |
| `MarketStateClassifier` is the standalone classifier | True, but disagrees with orchestrator (Finding #4) |
| `EquilibriumCompressionScorer` returns `ExitSignal` (HOLD / TIGHTEN / SCALE_OUT / EXIT_NOW) | True ✓ |
| `LongHorizonCalibrationAnalyzer` returns drift level + win rate + return + drawdown | True ✓ |
| "Pure functions only" | False at scorer-internal level (Finding #3 — notes mutation) |
| "Same input means same output" | False — `Instant.now()` (Finding #5) |
| "Keep helper classes top-level; no nested classes" | True ✓ |
| L3 = structural, L4 = classification, L5 = deployment, L6 = propagation, L7 = exit, L8 = meta | True at package level, but L3/L4/L5/L6 scorers are not invoked from orchestrator (Finding #1) |

---

## Strengths

1. **Sub-package organization matches MD layers exactly** as documented. A reader who knows MD-L4 looks in `services/classification/` and finds eight scorers, each named after a concept in the framework.
2. **`EquilibriumCompressionScorer` is the cleanest scorer in the module.** It's a true pure function, returns a value object containing notes, and is well-documented with javadoc explaining the additive contribution model.
3. **`AdversarialValidationAnalyzer` is a strong example of the "scores carry explanation data" principle** — every check returns an `AdversarialFinding` with type, weight, and rationale.
4. **`@JsonIgnoreProperties` is not abused** — none of the scorers leak Jackson concerns. lib-analytics has zero web/JSON dependencies.
5. **Sub-package `package-info.java`s exist** per ARCHITECTURE.md line 124, calling out the MD layer for each sub-package.
6. **`LongHorizonCalibrationAnalyzer` correctly handles the empty-samples case** without conflating it with "stable framework" (returns `STABLE` but with a "no samples" finding and recommendation).
7. **No static mutable state anywhere** in the module. Constants are `static final`; everything else flows through method parameters.
8. **Sub-package boundary tests exist** — even if Finding #13 notes they're at the wrong layer for catching orchestrator drift, the boundary coverage is genuine for the scorers that have direct callers.
9. **Pom is clean.** Only lib-math, lib-domain, lombok, junit. No Spring, no JPA, no Jackson. The library can be used outside the Spring context.
10. **The forward-looking META_ADAPTATION_FEEDBACK.md is explicitly marked unimplemented** (line 12), and the code does not pretend otherwise. Doc honesty is rare and valuable.

---

## Summary table — issue count by severity

| Severity | Count | Findings |
| --- | --- | --- |
| Critical | 2 | #1 (orchestrator reimplements scorers inline), #2 (orchestrator and standalone classifier disagree on regime label) |
| High | 2 | #3 (notes mutation violates pure-function rule), #4 (`MarketStateClassifier` disagrees with orchestrator on the same input — corollary of #2 but separately user-visible) |
| Medium | 7 | #5, #6, #7, #8, #9, #10, #11, #13 |
| Low | 2 | #12, #14 |
| **Total** | **13** | |

The two critical findings are linked: `DeterministicAnalyticsService` reimplements most documented scorers, and the reimplementation disagrees with the canonical version. Either the inline code or the scorer classes are dead. The fix is mechanical (delete or delegate) but the design decision about which side wins is load-bearing — the wrong choice cements the wrong implementation as the source of truth. The recommended approach: keep the scorer classes (they match the documented architecture), delete the inline duplicates, and add an integration test that exercises the orchestrator end-to-end against the scorers' boundary tests.
