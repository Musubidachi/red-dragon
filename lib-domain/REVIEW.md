# lib-domain — Implementation Review

**Date:** 2026-05-17
**Last fix pass:** 2026-05-20 (local fixes only)
**Reviewer:** Automated audit pass
**Scope:** `lib-domain/` — ~48 Java files (value objects, enums, one utility, one `exit/` sub-package). No module-local README exists; the spec is the parent `README.md` (line 43 + Project Conventions section) and parent `ARCHITECTURE.md`.

---

## Fix Status

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | High | Three score-validation strategies mixed | **Partially fixed** — `TradeCandidate` now delegates to `ValidationScoreUtils.requireNormalized`; cross-class strategy unification still open |
| 2 | Medium | Stringly-typed dimension/tier returns | Open — needs new `LiquidityTier` / `ScoreDimension` enums plus caller migration |
| 3 | Medium | Last-line accumulator bug in dimension chains | **Fixed** — `AnalyticsSnapshot.dominantScore`, `AnalyticsScoreBreakdown.weakestDimension/strongestDimension` now update both name and accumulator consistently |
| 4 | Medium | `MarketDataSnapshot` doesn't validate price/volume/gap fields | **Fixed** — `requireFiniteNonNegative` for prices/volumes/ATR, `requireFinite` for gapPercent/vwapDeviation/relativeVolume |
| 5 | Medium | `Instant.now()` in constructors breaks determinism | Open — fix would force callers to supply observedAt (breaks tests + production paths) |
| 6 | Low | Magic-number thresholds (0.65, 0.25) | Open — defers to ValidationThresholds, cross-module work |
| 7 | Low | `IngestionTextUtils` named for one module but lives here | Open — rename would touch every caller |
| 8 | Low | `Instant.now()` defeats `@Value` immutability | Open — corollary of #5 |
| 9 | Low | `MarketDataSnapshot` mixes clamp + `Math.max` | **Fixed** — `MarketMathUtils.floorAtZero` added to lib-math; `MarketDataSnapshot.relativeVolume` now routes through it (preserves the lower bound, rejects NaN that previously slipped through `Math.max(0.0, NaN) == NaN`) |
| 10 | Low | `AnalyticsSnapshot.regimeLabel` silently defaults to MIXED | **Fixed** — now `Objects.requireNonNull` |
| 11 | Low | `IngestionTextUtils` methods redundantly `static` | **Fixed** — dropped `static` (matches `@UtilityClass` style) |
| 12 | Low | `CandidateValidationInput` has 16 positional args | **Fixed** — `@Builder(toBuilder = true)` added; positional construction still compiles for existing callers |
| 13 | Low | Tests cover only 5 of ~48 value objects | Open — additive work |
| 14 | Low | Confusing snapshot pair naming | **Fixed (documented)** — javadoc on all three pairs (`Market*Snapshot` vs `*Snapshot`) explains the "with provider context" vs "score-only for L4 scorers" distinction and cross-links |

**Open: 7** — most are cross-module renames, enum migrations, or deferred determinism work the local pass intentionally did not touch.

---

---

## Expected Behavior (per docs)

Parent `README.md` line 43:

> `lib-domain` | shared | Shared value objects and enums that flow across modules: candidates, market snapshots, analytics snapshots, validation results, and verdict types.

Parent `README.md` lines 69–71:

> `lib-domain` is the exception: it owns shared domain language under `dev.reddragon.domain.models`. External API wire DTOs, controller DTOs, and JPA entities intentionally stay in their owning modules.

Conventions from `README.md` (lines 73–82) and `ARCHITECTURE.md` (lines 128–142):

- Lombok is the default: `@Value` + `@Accessors(fluent = true)` for value objects.
- **No nested classes.** Every class, enum, record, and interface lives in its own top-level file.
- **`@Value` constructors do validation.** "If a field has a normalized range, the constructor must enforce it — there is no second line of defense." (ARCHITECTURE.md line 141)
- Pure functions in `lib-analytics`; lib-domain feeds it, so value objects must be deterministic to construct.
- Shared cross-module value objects only.

---

## Files Reviewed

Organized by category. (Full enumeration in module pom build output; spot-checked representative files in depth.)

| Category | Representative files | Count |
| --- | --- | --- |
| Trade candidate | `TradeCandidate.java`, `CandidateCatalystType.java`, `SourceType.java` | 3 |
| Market data | `MarketBar.java`, `IntradayBar.java`, `MarketDataSnapshot.java`, `MarketDataEvent.java`, `MarketDataQuality.java`, `MarketQuote.java`, `MarketReplayFrame.java`, `OrderBookSnapshot.java` | 8 |
| Market state (L4) | `MarketStateSignal.java`, `MarketIntradayStructureSnapshot.java`, `IntradayStructureSnapshot.java`, `MarketLiquidityTextureSnapshot.java`, `LiquidityTextureSnapshot.java`, `MarketVolatilityExpansionSnapshot.java`, `VolatilityExpansionSnapshot.java`, `OptionsFlowSnapshot.java`, `RegimeLabel.java`, `PhaseLabel.java`, `PhaseTransitionSnapshot.java` | 11 |
| Analytics | `AnalyticsSnapshot.java`, `AnalyticsScoreBreakdown.java`, `PropagationSnapshot.java`, `FundamentalImpactSnapshot.java` | 4 |
| Validation | `CandidateValidationInput.java`, `ValidationResult.java`, `ValidationFactor.java`, `ValidationAudit.java`, `ValidationStage.java`, `ValidationSummary.java`, `Verdict.java`, `VerdictDecision.java`, `DeploymentTier.java`, `ReasonCode.java`, `RiskFlag.java` | 11 |
| Adversarial | `AdversarialFinding.java`, `AdversarialFindingType.java` | 2 |
| Calibration | `CalibrationDriftLevel.java`, `CalibrationReport.java`, `OutcomeSample.java` | 3 |
| Trade modification | `TradeModificationAction.java`, `TradeModificationRequest.java` | 2 |
| Exit (L7) sub-package | `exit/ExitRecommendation.java`, `exit/ExitSignal.java`, `exit/ExitSignalInput.java` | 3 |
| Utility | `utilities/IngestionTextUtils.java` | 1 |
| Tests (`src/test/`) | `TradeCandidateTest.java`, `MarketDataSnapshotTest.java`, `ValidationResultTest.java`, `OutcomeSampleTest.java`, `exit/ExitSignalTest.java` | 5 |

---

## Findings

### 1. Three different score-validation strategies are mixed across nearly-identical value objects

**Severity:** High
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\TradeCandidate.java:88-104` (private inline `requireNormalized`, throws)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsSnapshot.java:46-50` (`AnalyticsScoreUtils.clamp`, lenient)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsScoreBreakdown.java:32-39` (`AnalyticsScoreUtils.clamp`, lenient)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\CandidateValidationInput.java:84-91` (`ValidationScoreUtils.requireNormalized`, throws)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\ValidationResult.java:39` (`ValidationScoreUtils.requireNormalized`, throws)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\MarketDataSnapshot.java:62-68` (`MarketMathUtils.clamp`, lenient)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\exit\ExitSignal.java:43` (`AnalyticsScoreUtils.clamp`, lenient)

**Expected vs Actual:** ARCHITECTURE.md line 141 says: *"If a field has a normalized range, the constructor must enforce it — there is no second line of defense."* The doc does not mandate clamp-vs-throw, but it implies a single house style. Actual: three styles co-exist:

- **Throw strict (`ValidationScoreUtils.requireNormalized`)** — used by `CandidateValidationInput`, `ValidationResult`.
- **Clamp lenient (`AnalyticsScoreUtils.clamp`, `MarketMathUtils.clamp`)** — used by `AnalyticsSnapshot`, `AnalyticsScoreBreakdown`, `MarketDataSnapshot`, `ExitSignal`.
- **Throw strict (re-implemented inline)** — `TradeCandidate.requireNormalized` is a private static duplicate of `ValidationScoreUtils.requireNormalized`:

```java
// TradeCandidate.java:99-104
private static double requireNormalized(String fieldName, double value) {
    if (value < 0.0 || value > 1.0) {
        throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
    }
    return value;
}
```

This is the strict version of what's already imported by sibling files via `dev.reddragon.math.ValidationScoreUtils`. `lib-domain/pom.xml` line 22 already depends on `lib-math`.

**Why it matters:** Two correctness issues fall out:

1. **A score of `1.1` produces different observable behavior depending on which value object touches it first.** `AnalyticsSnapshot` receiving `1.1` will silently clamp to `1.0`. `ValidationResult` receiving the same value will throw. The same numeric ingress can succeed or fail by accident of which object holds it.
2. **The flagship value object (TradeCandidate) maintains its own forked validator.** A future fix to `ValidationScoreUtils.requireNormalized` (e.g., NaN handling per the lib-math review) will not propagate to `TradeCandidate`.

**Proposed fix:** Pick one strategy per *semantic* category:
- Score inputs supplied *by* the user / API / config — throw (caller error).
- Score inputs from *internal* deterministic computations — either clamp (assume scorer can drift slightly) or trust the contract and skip validation.

Document the choice in `package-info.java`. Delete `TradeCandidate.requireNormalized` and use `ValidationScoreUtils.requireNormalized` instead.

---

### 2. `MarketDataSnapshot.liquidityTier()` and `AnalyticsSnapshot.dominantScore()` and `AnalyticsScoreBreakdown.weakestDimension/strongestDimension` return Stringly-typed dimension names

**Severity:** Medium
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\MarketDataSnapshot.java:94-99`
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsSnapshot.java:58-67`
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsScoreBreakdown.java:49-75`

**Expected vs Actual:** README characterizes lib-domain as the home for "value objects and enums." The right tool for a closed set of named buckets is an enum. Actual:

```java
// MarketDataSnapshot.java:94-99
public String liquidityTier() {
    if (averageVolume >= 1_000_000) return "HIGH";
    if (averageVolume >= 250_000)   return "MODERATE";
    if (averageVolume >= 50_000)    return "ADEQUATE";
    return "THIN";
}
```

```java
// AnalyticsScoreBreakdown.java:64-75
public String strongestDimension() {
    double max = structuralRealityScore;
    String name = "structuralReality";
    if (materialSignificanceScore > max)  { max = materialSignificanceScore;  name = "materialSignificance"; }
    ...
}
```

Downstream callers must compare on string equality (`"HIGH".equals(snapshot.liquidityTier())`), with no compile-time guarantee that the constant is spelled right.

**Why it matters:** A typo (`"high"` vs `"HIGH"`, `"asymmetry"` vs `"Asymmetry"`) is a silent runtime miss. The codebase already has the right enums conceptually nearby (e.g. there's a `DeploymentTier` enum for capital tiers, but no `LiquidityTier` enum for liquidity tiers despite the same pattern).

**Proposed fix:** Add `LiquidityTier` (`HIGH | MODERATE | ADEQUATE | THIN`) and `ScoreDimension` enums. Return the enum, expose a `displayName()` on it for human surfaces. Migrate callers.

---

### 3. `AnalyticsScoreBreakdown.weakestDimension/strongestDimension` and `AnalyticsSnapshot.dominantScore` have a latent "forgot to update accumulator" bug

**Severity:** Medium
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsScoreBreakdown.java:50-61` (`weakestDimension`)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsScoreBreakdown.java:64-75` (`strongestDimension`)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsSnapshot.java:58-67` (`dominantScore`)

**Expected vs Actual:** All three functions implement an unrolled max/min scan. Each line follows the pattern `if (score > max) { max = score; name = "X"; }`, except the *last* line, which deliberately omits `max = score` (presumably because the variable is no longer needed):

```java
// AnalyticsScoreBreakdown.java:59 — last comparison
if (deploymentConfidenceScore > max)  {                                    name = "deploymentConfidence"; }
```

The result is *correct today*. But the inconsistency is invisible (no comment), and adding a new score dimension to either record would require the author to remember to (a) extend the chain, (b) update the previous last-line to also assign to `max`, and (c) drop the assignment in the new last-line. Three steps; easy to get one wrong.

**Why it matters:** Latent bug surface for future score additions. The L8 calibration analysis (`META_ADAPTATION_FEEDBACK.md`) flags drift in specific dimensions; if a new dimension is added later (e.g. for sector propagation), the chain extension is fragile.

**Proposed fix:** Replace with a `Stream.of(entry, entry, ...).max(Comparator.comparingDouble(...)).orElseThrow()` pattern, or build a `List<DimensionScore>` of pairs and use `Collections.max`. Either form is single-source-of-truth and immune to new-dimension drift. Or, simpler: extract each `if` into a helper that always updates both `max` and `name`.

---

### 4. `MarketDataSnapshot` does not validate non-score numeric fields, silently accepting nonsensical values

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\MarketDataSnapshot.java:36-71`

**Expected vs Actual:** ARCHITECTURE.md line 141 carves the rule to "fields with a normalized range," but the field set includes prices, volumes, and gap percentages where negative or NaN values are physically meaningless. Actual:

```java
this.latestClose = latestClose;
this.previousClose = previousClose;
this.gapPercent = gapPercent;
this.averageTrueRange = averageTrueRange;
...
this.averageVolume = averageVolume;
...
this.vwapDeviation = vwapDeviation;
```

No checks. `new MarketDataSnapshot("AAPL", now, -1.0, 0.0, NaN, -7.0, ...)` is legal. The unchecked NaN can then poison downstream `safePercentChange(latestClose, previousClose)` etc.

**Why it matters:** `MarketDataSnapshot` is a load-bearing object that flows into `lib-analytics` scorers and the L7 exit signal. Garbage-in, garbage-out, but the absence of validation means the garbage doesn't surface until a verdict is rendered, by which point the candidate id has been logged as PASS/WATCH/REJECT.

**Proposed fix:** Add per-field guards: `Math.max(0.0, latestClose)` if you want lenience, or `if (latestClose < 0 || !Double.isFinite(latestClose)) throw` if strict. The previous review's lib-math findings call for `lib-math.MarketMathUtils.requirePositivePrice(...)` or similar — adding such a helper to lib-math and using it here would solve both libs at once.

---

### 5. Multiple value-object constructors fall back to `Instant.now()` for `observedAt`, breaking deterministic construction

**Severity:** Medium
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\TradeCandidate.java:85`
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\MarketDataSnapshot.java:57`
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsSnapshot.java:44`

**Expected vs Actual:** ARCHITECTURE.md line 137: "Each scorer takes a snapshot in and returns a 0.0–1.0 score out." The whole point of immutable value objects is reproducibility: `new TradeCandidate(builder)` constructed identically twice should produce two equal objects. Actual:

```java
// TradeCandidate.java:85
this.observedAt = observedAt == null ? Instant.now() : observedAt;
```

`Instant.now()` is wall-clock dependent. Two `TradeCandidate.builder().build()` calls in quick succession will produce two non-equal candidates (different `observedAt`), so `equals/hashCode` becomes time-dependent and tests cannot easily assert structural equality.

**Why it matters:** Two breakage modes:

1. **Test determinism.** `lib-backtest` claims "identical inputs must produce identical outputs" but the inputs are built by callers using `.builder().build()` — if the caller doesn't set `observedAt`, the backtest engine's deterministic claim is broken at the input boundary.
2. **Cache keys / deduplication.** `lib-persistence` may use `equals` for upsert logic; two saves of "the same candidate" with auto-filled `observedAt` look different.

**Proposed fix:** Require `observedAt` to be explicitly provided — `Objects.requireNonNull(observedAt, ...)`. If a default is wanted, accept an injected `Clock` via a factory method (e.g., `TradeCandidate.observedNow(builder, clock)`), don't bake `Instant.now()` into the constructor.

---

### 6. `TradeCandidate.hasCredibleStructuralCatalyst()` and `MarketDataSnapshot.isHostile()` embed magic-number thresholds

**Severity:** Low
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\TradeCandidate.java:94-97` (`0.65`)
- `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\MarketDataSnapshot.java:81-85` (`0.25`)

**Expected vs Actual:** lib-validation owns thresholds (per ARCHITECTURE.md L3/L5). Value objects in lib-domain should be data-only.

```java
// TradeCandidate.java
public boolean hasCredibleStructuralCatalyst() {
    return structuralRealityScore >= 0.65;
}
```

```java
// MarketDataSnapshot.java
public boolean isHostile() {
    return quality == MarketDataQuality.ILLIQUID
            || quality == MarketDataQuality.STALE
            || volatilityStabilityScore < 0.25;
}
```

The `0.65` and `0.25` are not in `ValidationThresholds` and cannot be tuned per validation profile.

**Why it matters:** The threshold drift risk described in lib-backtest REVIEW.md Finding #1 also lives here. `hasCredibleStructuralCatalyst` is consumed by `CandidatePipelineOrchestrator` *and* `BacktestReplayEngine` (both pass it into `CandidateValidationInput.credibleCatalyst`). A profile change in lib-validation does not affect either of those wirings.

**Proposed fix:** Move both thresholds into `ValidationThresholds`. Replace the predicates with deprecated no-op stubs or remove them and let callers consult thresholds directly.

---

### 7. `IngestionTextUtils` is named for one module ("ingestion") but lives in lib-domain

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\utilities\IngestionTextUtils.java`

**Expected vs Actual:** README line 43 defines lib-domain as the home for "Shared value objects and enums that flow across modules." `IngestionTextUtils` is a utility class, not a value object or enum — its presence here is already a soft violation. Its name then doubles down on the violation by claiming an `ingestion` concern.

```java
@UtilityClass
public class IngestionTextUtils {
    public static String clean(String value) { return value == null ? "" : value.trim(); }
    public static String requireText(String value, String fieldName) { ... }
    public static String normalizeSymbol(String symbol) { ... }
}
```

The class is called by `TradeCandidate.java:78-87` (six call sites) and only `TradeCandidate` uses it directly. Several other lib-domain models reinvent `symbol.trim().toUpperCase()` rather than call `normalizeSymbol`:

- `MarketDataSnapshot.java:56` — `this.symbol = symbol.trim().toUpperCase();`
- `AnalyticsSnapshot.java:43` — `this.symbol = symbol.trim().toUpperCase();`

So the utility lives in the wrong-named place *and* its callers don't all use it.

**Why it matters:** Two parallel concerns: (a) the package boundary is muddy — readers searching `lib-ingestion` for ingestion utilities will not find this; (b) duplicate symbol-normalization logic exists, ripe for one-side fixes (e.g. someone adds NFKC unicode normalization to `IngestionTextUtils.normalizeSymbol` but the two inline copies still produce `"BTC​"` ZWSP-suffixed symbols).

**Proposed fix:** Either rename to `DomainTextUtils` (or `TextNormalization`) and use it consistently from every symbol-normalizing constructor, or move it into `lib-ingestion` and have lib-domain define its own minimal symbol normalizer (it would still need one for `TradeCandidate`).

---

### 8. `Instant.now()` side effect in constructor also defeats the `@Value` immutability claim

**Severity:** Low
**Files:** as Finding #5.

**Why it matters (separate from #5):** `@Value` says "this is a value object." A value object's identity should be derivable from its observable fields. With `observedAt = Instant.now()` as fallback, two structurally-identical builder calls produce two non-equal objects. `equals/hashCode` from `@Value` work correctly for the *result*, but the constructor is impure. Reviewer comments aside, this contradicts the spirit of `@Value` and the determinism convention.

**Proposed fix:** See Finding #5.

---

### 9. `MarketDataSnapshot` mixes `MarketMathUtils.clamp` with manual `Math.max(0.0, ...)` for similar semantic fields

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\MarketDataSnapshot.java:62-68`

**Expected vs Actual:**

```java
this.rangePosition = MarketMathUtils.clamp(rangePosition);                      // 0..1 clamp
this.liquidityScore = MarketMathUtils.clamp(liquidityScore);                    // 0..1 clamp
this.volatilityStabilityScore = MarketMathUtils.clamp(volatilityStabilityScore);// 0..1 clamp
this.relativeVolume = Math.max(0.0, relativeVolume);                            // 0..∞ floor
this.vwapDeviation = vwapDeviation;                                             // unbounded
this.directionalPersistence = MarketMathUtils.clamp(directionalPersistence);    // 0..1 clamp
```

`relativeVolume` correctly is *not* clamped to 1.0 (a heavy day has relVol > 1.0). But the floor is done with raw `Math.max(0.0, ...)` rather than a shared helper. lib-math does not currently expose a `floorAtZero` helper. The result is an ad-hoc one-liner that the next contributor will copy-and-paste with a typo.

**Why it matters:** Three places in the codebase now use the same `Math.max(0.0, ...)` floor idiom for non-clampable doubles. They will drift independently. The lib-math review's recommendation to consolidate clamp / average / weightedAverage into a single `Numerics` class should be extended to include `floorAtZero(double)`.

**Proposed fix:** Add `MarketMathUtils.floorAtZero(double)` (or rename to `nonNegative`); call it here.

---

### 10. `AnalyticsSnapshot.regimeLabel` silently defaults to `MIXED` if null is passed

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\AnalyticsSnapshot.java:45`

**Expected vs Actual:** `this.regimeLabel = regimeLabel == null ? RegimeLabel.MIXED : regimeLabel;` masks a programmer error (forgot to compute regime) as a legitimate "mixed" label. `MIXED` is a real classification state for actual mixed regimes; conflating "I didn't compute" with "I computed MIXED" loses signal.

**Why it matters:** Calibration reports rely on `regimeLabel` to attribute wins/losses to specific regimes. Silently-MIXED candidates will contaminate the MIXED bucket. Per `META_ADAPTATION_FEEDBACK.md`, regime-conditioned drift is a primary L8 metric.

**Proposed fix:** `Objects.requireNonNull(regimeLabel, "regimeLabel is required")`. Or add an explicit `RegimeLabel.UNKNOWN` sentinel and use that as the default, so the bucket separates from real MIXED data.

---

### 11. `IngestionTextUtils` methods are redundantly declared `static` even though `@UtilityClass` makes them implicitly static

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\utilities\IngestionTextUtils.java:11-25`

**Expected vs Actual:** Lombok `@UtilityClass` already makes every member `static`. Adding `static` to each method is harmless but inconsistent with `lib-math/AnalyticsScoreUtils.java` (which omits the redundant keyword). Code-style drift across two utility-class home pages.

**Why it matters:** Cosmetic. Suggests the author wasn't sure how `@UtilityClass` works, which can spread to other future utility classes.

**Proposed fix:** Drop the explicit `static` from each method to match lib-math's style.

---

### 12. `CandidateValidationInput` has 16 positional arguments — a builder is missing

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-domain\src\main\java\dev\reddragon\domain\models\CandidateValidationInput.java:63-104`

**Expected vs Actual:** `@Value @Accessors(fluent = true)` with 8 double-score fields and 5 boolean flags. The only constructor takes 16 positional arguments. There is no `@Builder` annotation. This is exactly the construction pattern that produces the lib-backtest Finding #5 fragility:

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
        ...
);
```

A reorder of two same-typed fields in this record compiles silently and produces subtly-wrong verdicts.

**Why it matters:** Two places already construct this (`CandidatePipelineOrchestrator` and `BacktestReplayEngine`); both are positional and brittle.

**Proposed fix:** Add `@Builder` (Lombok). `TradeCandidate` already does this (line 61, `@Builder(toBuilder = true)`). Migrate the two callers.

---

### 13. Tests cover only 5 of ~48 value objects; many enums have no test

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-domain\src\test\java\dev\reddragon\domain\models\`

**Expected vs Actual:** Five test files:

```
TradeCandidateTest.java
MarketDataSnapshotTest.java
ValidationResultTest.java
OutcomeSampleTest.java
exit/ExitSignalTest.java
```

No tests for `CandidateValidationInput` (the 16-arg landmine), `AnalyticsSnapshot`, `AnalyticsScoreBreakdown`, the seven snapshot variants, `Verdict.displayName`, `DeploymentTier.isActionable`, `RegimeLabel.isSupportive`, `ExitSignalInput`, or `ExitRecommendation`. Most enums have `displayName()` switches that would silently fail on a new enum constant if `default` is removed (currently exhaustive, so fine — until someone adds an enum value).

**Why it matters:** The "no second line of defense" ARCHITECTURE.md rule places the entire correctness burden on `@Value` constructors. Constructors that aren't unit tested are not enforced.

**Proposed fix:** Add tests for at least:
- `CandidateValidationInput` — all 8 score fields trigger `IllegalArgumentException` at the bounds.
- Each enum's `displayName()` returns non-null for every value (loop with `assertNotNull` over `values()`).

---

### 14. Two snapshot pairs with very similar names (`MarketIntradayStructureSnapshot` vs `IntradayStructureSnapshot`, `MarketLiquidityTextureSnapshot` vs `LiquidityTextureSnapshot`, `MarketVolatilityExpansionSnapshot` vs `VolatilityExpansionSnapshot`) are easy to confuse

**Severity:** Low
**Files:** the six listed in `Files Reviewed`.

**Expected vs Actual:** Three pairs of snapshot types differ only by the `Market` prefix. Naming convention is unclear: is `Market*` the broader index-level view and unprefixed the per-symbol view? Or vice versa? Neither file has javadoc explaining the difference. A reader has to read both to figure out which one to use.

**Why it matters:** Cross-module callers (lib-marketdata builds these, lib-analytics consumes them) may pick the wrong type. Compiler will not catch the mistake until the consumer asks for a field that exists in only one of the two.

**Proposed fix:** Either (a) merge each pair if they truly represent the same concept at different scopes (add a `MarketScope { INDEX | SYMBOL }` field), or (b) keep them separate but add javadoc on each class explaining the distinction and pointing at the other one. Recommend (b) at minimum.

---

## Domain Leakage Audit

Per README line 43, lib-domain is for *shared* cross-module value objects only.

| Type | Used by | Justified as cross-module? |
| --- | --- | --- |
| `TradeCandidate` | lib-ingestion (producer), lib-analytics, lib-validation, lib-backtest, app | ✓ |
| `MarketDataSnapshot` | lib-marketdata (producer), lib-analytics, lib-validation, lib-backtest | ✓ |
| `AnalyticsSnapshot` | lib-analytics (producer), lib-validation, lib-backtest, app | ✓ |
| `ValidationResult` | lib-validation (producer), lib-backtest, app | ✓ |
| Exit sub-package | lib-analytics (producer), app | ✓ |
| `MarketReplayFrame` | lib-marketdata, lib-backtest? | borderline — is this used outside lib-marketdata? |
| `OrderBookSnapshot` | Schwab market-data adapter? | borderline — could be package-private in lib-marketdata. |
| `IngestionTextUtils` | TradeCandidate only | Not a value object, name advertises lib-ingestion (Finding #7). |

No JPA `@Entity`, no Spring web `@RequestBody` DTOs, no Schwab-specific DTOs found in lib-domain. The "external DTOs intentionally stay in their owning modules" rule (README line 70–71) is honored. ✓

---

## Strengths

1. **`@Value + @Accessors(fluent = true)` is used uniformly** on every value object inspected. Not one class falls back to plain Java with manual getters.
2. **No nested classes anywhere.** The Grep for `^    (public |private |static )?(class|enum|interface|record)` returned zero results. The convention is honored module-wide.
3. **Defensive `List.copyOf` for every list field** in `AnalyticsSnapshot`, `MarketDataSnapshot`, `ValidationResult`, `ExitSignal`, `TradeCandidate` (transitively). Immutability is real, not just nominal.
4. **Enums carry their own `displayName()` and behavior predicates** (`Verdict.isReviewable`, `DeploymentTier.isActionable`, `RegimeLabel.isSupportive`, `ExitRecommendation` enum well-documented). UI rendering does not need a separate display layer.
5. **`@Value` + `@Builder(toBuilder = true)` on `TradeCandidate`** is exactly the pattern recommended by ARCHITECTURE.md. The "copy with modifications" idiom works cleanly for pipeline transformations.
6. **`ExitSignal / ExitSignalInput / ExitRecommendation` form a coherent triple** for L7, with extensive javadoc and clear naming. The sub-package separation is well done.
7. **Module pom is tight.** Only `lib-math` + Lombok + JUnit. No Spring at this layer, no JPA, no Jackson. Exactly the foundation a shared-types module should look like.
8. **Validation results expose helpful convenience predicates** (`passed()`, `rejected()`, `watch()`, `isActionable()`, `primaryReason()`) rather than forcing UI code to inspect the underlying enum.
9. **`CandidateValidationInput` fields are individually javadoc'd** with what each score means and where in the framework it originates. Good onboarding signal.

---

## Summary table — issue count by severity

| Severity | Count | Findings |
| --- | --- | --- |
| Critical | 0 | — |
| High | 1 | #1 (three score-validation strategies mixed) |
| Medium | 4 | #2 (stringly-typed dimensions), #3 (last-line-omitted accumulator), #4 (unchecked price/volume fields), #5 (`Instant.now()` in constructor) |
| Low | 9 | #6, #7, #8, #9, #10, #11, #12, #13, #14 |
| **Total** | **14** | |

The most consequential pattern is **Finding #1**: the same field type (a normalized 0..1 score) gets three different enforcement strategies across siblings. Picking one (or making the choice intentional and documented per-class) unlocks fixes to Findings #4, #11 by removing the precedent for ad-hoc validators. **Finding #5** (`Instant.now()` in constructors) is the single change with the largest blast radius — fixing it would make `equals`/`hashCode` deterministic across the codebase and unblock the determinism tests called for in the lib-backtest review.
