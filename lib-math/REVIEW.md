# lib-math — Implementation Review

**Date:** 2026-05-17
**Last fix pass:** 2026-05-21 (added `floorAtZero` helper; earlier 2026-05-20 pass covered NaN guards and `safePercentChange`). Duplicate consolidation and package move still deferred.
**Reviewer:** Automated audit pass
**Scope:** `lib-math/` module (3 utility classes, package-info, pom, 3 test classes)

---

## Fix Status

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | High | `clamp(NaN)` returns NaN | **Fixed** — both `AnalyticsScoreUtils.clamp` and `MarketMathUtils.clamp` now throw `IllegalArgumentException` on NaN |
| 2 | Medium | Duplicate `clamp`/`average`/`weightedAverage` | Open — deferred to a follow-up consolidation pass |
| 3 | Medium | `safePercentChange` denominator edge cases | **Fixed** — rejects non-finite or non-positive `previous` and non-finite `current` |
| 4 | Low | Package layout violates `utilities/` convention | Open — deferred (would require updating ~40 import statements across other libs) |
| 5 | Low | Module pom description drops "percent-change" | **Fixed** — verbatim match to parent README wording |
| 6 | Low | `requireNormalized` silently accepts NaN | **Fixed** — explicit `Double.isNaN` check with field-named message |
| 7 | Low | `requireNonNegative` accepts NaN and +Infinity | **Fixed** — `!(value >= 0.0 && Double.isFinite(value))` rejects both |
| 8 | Low | `weightedAverage` returns 0.0 ambiguously | Open — behavior preserved, documentation could be improved |
| 9 | Low | Tests miss `assertEquals` delta in places | **Fixed** — every double assertion uses `1e-9` |
| 10 | — | (withdrawn during original review) | — |
| 11 | Low | No NaN/Infinity tests for the require methods | **Fixed** — `clampRejectsNaN`, `requireNormalizedRejectsNaN`, `requireNonNegativeRejectsNaN`, `requireNonNegativeRejectsPositiveInfinity` |
| 12 | Low | `package-info.java` narrower than parent README | **Fixed** — full four-pillar description plus clamp-vs-require split note |
| — | New | `floorAtZero` helper added | **New API** — `MarketMathUtils.floorAtZero(double)` rejects NaN, preserves positive infinity; used by `lib-domain.MarketDataSnapshot.relativeVolume` |

**Open: 3** — duplicate consolidation, package-move convention, `weightedAverage` ambiguity. All require either cross-module work or a design decision the local pass intentionally did not make.

---

---

## Expected Behavior (per docs)

Parent `pom.xml` (lines 18–31) declares the module as:

> `lib-math        - shared numeric helpers for score normalization and simple market math`

Root `README.md` (Module Layout, line 42):

> `lib-math` | shared | Numeric helpers for clamping, normalized-score validation, weighted averages, and percent-change math.

`ARCHITECTURE.md` (line 57) reaffirms:

> `lib-math` | Shared numeric helpers for score normalization, weighted averages, clamping, and percent-change math | shared

Conventions from `README.md` (lines 73–82) and `ARCHITECTURE.md` (lines 128–142) that apply to this module:

> * Lombok is the default for value objects, service constructors, and loggers.
> * No nested classes. Every class, enum, record, and interface lives in its own top-level file.
> * Shared numeric helpers live in `lib-math`.
> * `lib-analytics` is deterministic and side-effect free. (lib-math, as its upstream dep, must also be pure.)

`ARCHITECTURE.md` package convention (lines 14–22) prescribes the standard
module shape with `models/`, `services/`, `utilities/`, `config/` subdirs;
`utilities/` is the named home for "pure static helpers."

The package's own `package-info.java` (line 2) restates the charter:

> `Shared numeric helpers for score normalization and simple market math.`

Module pom `description` (line 17) narrows further:

> `Small shared numeric helpers for normalization, clamping, and weighted scores.`
> (Note: this drops "percent-change math" relative to the parent README.)

---

## Files Reviewed

| File | Purpose |
| --- | --- |
| `C:\repos\red-dragon\red-dragon\lib-math\pom.xml` | Maven module descriptor; declares lombok (provided) and junit-jupiter (test). |
| `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\package-info.java` | One-line package javadoc. |
| `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\AnalyticsScoreUtils.java` | `@UtilityClass` providing `clamp`, `average`, `weightedAverage`. |
| `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\MarketMathUtils.java` | `@UtilityClass` providing `clamp`, `safePercentChange`, `average`. |
| `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\ValidationScoreUtils.java` | `@UtilityClass` providing `requireNormalized`, `requireNonNegative`, `weightedAverage`. |
| `C:\repos\red-dragon\red-dragon\lib-math\src\test\java\dev\reddragon\math\AnalyticsScoreUtilsTest.java` | JUnit 5 smoke tests covering clamp / average / weightedAverage. |
| `C:\repos\red-dragon\red-dragon\lib-math\src\test\java\dev\reddragon\math\MarketMathUtilsTest.java` | JUnit 5 smoke tests covering clamp / safePercentChange / average. |
| `C:\repos\red-dragon\red-dragon\lib-math\src\test\java\dev\reddragon\math\ValidationScoreUtilsTest.java` | JUnit 5 smoke tests covering requireNormalized / requireNonNegative / weightedAverage. |

---

## Findings

### 1. `clamp(NaN)` silently returns NaN and propagates into normalized snapshots

**Severity:** High
**Files:** `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\MarketMathUtils.java:11-19`, `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\AnalyticsScoreUtils.java:11-19`

**Expected vs Actual:** Per ARCHITECTURE.md ("If a field has a normalized range, the constructor must enforce it — there is no second line of defense"), `clamp` is the constructor-side guardrail for every `MarketDataSnapshot`, `MarketLiquidityTextureSnapshot`, `MarketIntradayStructureSnapshot`, etc. (see usages in `lib-domain/models/Market*.java`). The contract should produce a value in `[0.0, 1.0]`. Actual: when called with `Double.NaN`, both comparisons (`value < 0.0` and `value > 1.0`) evaluate `false`, so the function returns `NaN`, breaking the "no second line of defense" invariant.

```java
public double clamp(double value) {
    if (value < 0.0) {
        return 0.0;
    }
    if (value > 1.0) {
        return 1.0;
    }
    return value;
}
```

**Why it matters:** `MarketFeatureCalculator.safePercentChange(latestBar.open(), previousClose)` and the various ratio calculations feeding `clamp(...)` callers can produce NaN if any upstream input is NaN (e.g. missing prior close, zero-division masked by `Infinity * 0.0`, or `Math.log` on zero). A `NaN` score then poisons every downstream weighted aggregation in `DisequilibriumValidationEngine.weightedAverage(...)` because `NaN + x == NaN`. None of this is currently caught.

**Proposed fix:** Treat `NaN` as an explicit failure mode. Either throw `IllegalArgumentException("clamp: NaN")` (matches `ValidationScoreUtils.requireNormalized` style) or coerce to `0.0` with a documented rationale. Add a test asserting the chosen behavior.

---

### 2. Two identical `clamp` implementations (and two identical `average` implementations) duplicated across utility classes

**Severity:** Medium
**Files:** `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\AnalyticsScoreUtils.java:11-23`, `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\MarketMathUtils.java:11-27`

**Expected vs Actual:** README line 42 lists *one* set of helpers ("clamping, normalized-score validation, weighted averages, percent-change math"), implying one canonical implementation per operation. Actual: `clamp(double)` is byte-identical in both classes, and `average(double,double)` is byte-identical in both classes. `weightedAverage` is duplicated between `AnalyticsScoreUtils` and `ValidationScoreUtils` as well.

`AnalyticsScoreUtils.clamp`:

```java
public double clamp(double value) {
    if (value < 0.0) {
        return 0.0;
    }
    if (value > 1.0) {
        return 1.0;
    }
    return value;
}
```

`MarketMathUtils.clamp`:

```java
public double clamp(double value) {
    if (value < 0.0) {
        return 0.0;
    }
    if (value > 1.0) {
        return 1.0;
    }
    return value;
}
```

**Why it matters:** A future fix (e.g. adding NaN guarding from Finding #1) must be made in three places. Call sites already mix the two: `MarketMathUtils.clamp` is used from both `lib-marketdata` services and `lib-domain` value objects (legitimate), but `lib-analytics` scorers reach for `MarketMathUtils.clamp` in some places and `AnalyticsScoreUtils.clamp` in others — there is no consistent rule. Documentation calls these "shared helpers," not "namespace-specific helpers."

**Proposed fix:** Promote `clamp` and `average` to a single `Numerics` (or rename `AnalyticsScoreUtils` to `Scores`) and delete the duplicates. Keep `ValidationScoreUtils` strictly for the throwing variants (`requireNormalized`, `requireNonNegative`) since those are semantically distinct. If duplication must be retained for layering reasons, document why in `package-info.java`.

---

### 3. `safePercentChange` silently masks a negative-denominator sign flip and uses fragile `== 0.0` check

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\MarketMathUtils.java:21-23`

**Expected vs Actual:** README documents "percent-change math." Convention in finance is `(current - previous) / |previous|` *or* explicitly require `previous > 0`. Actual:

```java
public double safePercentChange(double current, double previous) {
    return previous == 0.0 ? 0.0 : (current - previous) / previous;
}
```

Two issues:
1. `previous == 0.0` is `false` when `previous == -0.0` is passed in (actually true via IEEE rules — `0.0 == -0.0` — so this part is fine), but it is also false for very-small denominators like `1e-300`, where the division overflows to `±Infinity`. No `Double.isFinite` guard.
2. If `previous` is negative (cannot happen for prices but `safePercentChange` is documented generically and `MarketMathUtils` is used for non-price ratios too), the sign of the resulting fraction is flipped relative to what most call sites expect. `safePercentChange(110, -100) = -2.1`, not `+2.1`.

**Why it matters:** The single production caller today is `MarketFeatureCalculator.safePercentChange(latestBar.open(), previousClose)` (line 103) where `previousClose` is a non-negative price, so issue 2 is latent. But the helper is in a "shared" module by README. Issue 1 (subnormal denominator -> Infinity -> downstream NaN via `0 * Inf` in weighted sums) is reachable today through micro-cap penny-stock prices.

**Proposed fix:** Treat both `previous == 0.0` and `!Double.isFinite(previous - 0.0)` as the "return 0.0" case, or throw on non-finite/zero/negative `previous`. Add tests for `previous = 1e-300`, `previous = -100`, and `current = NaN`.

---

### 4. Module's package layout violates the `utilities/` convention from ARCHITECTURE.md

**Severity:** Low
**Files:** `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\AnalyticsScoreUtils.java` (and siblings)

**Expected vs Actual:** ARCHITECTURE.md (lines 14–22) prescribes the module shape:

```
<module>/src/main/java/dev/reddragon/<module>/
    utilities/   — pure static helpers
```

Actual: All three utility classes live directly under `dev.reddragon.math` with no `utilities/` subpackage:

```
dev/reddragon/math/
    AnalyticsScoreUtils.java
    MarketMathUtils.java
    ValidationScoreUtils.java
    package-info.java
```

**Why it matters:** README.md line 79 explicitly carves out `lib-domain` as the exception to the standard layout but does *not* carve out `lib-math`, suggesting `lib-math` is supposed to follow the convention. Junior-developer onboarding (ARCHITECTURE.md "Junior-developer reading order") will be confused that the only module whose entire purpose is "pure static helpers" doesn't use the `utilities/` package.

**Proposed fix:** Either move the three classes to `dev.reddragon.math.utilities` (and update package-info), or amend ARCHITECTURE.md / README to note `lib-math` is a second documented exception alongside `lib-domain`.

---

### 5. Module pom `description` drops "percent-change" from the spec

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-math\pom.xml:17`

**Expected vs Actual:** Parent README and ARCHITECTURE both list "percent-change math" as one of four pillars. Module pom describes itself as:

```xml
<description>Small shared numeric helpers for normalization, clamping, and weighted scores.</description>
```

That omits percent-change, even though `MarketMathUtils.safePercentChange` exists and is the only place the operation is centralized.

**Why it matters:** The module description is what shows in IDE Maven views and any generated site docs; it is the third source of truth after the parent pom and the parent README. Drift here is small but compounds with Finding #4 to make the module feel under-documented for what is actually a load-bearing shared library.

**Proposed fix:** Replace the description with: `Shared numeric helpers for clamping, normalized-score validation, weighted averages, and percent-change math.` (verbatim from parent README line 42).

---

### 6. `requireNormalized` rejects NaN with a misleading message

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\ValidationScoreUtils.java:11-16`

**Expected vs Actual:** `requireNormalized` is the throwing counterpart to `clamp`. Unlike `clamp` (Finding #1), it *does* reject NaN — because `NaN < 0.0` and `NaN > 1.0` both evaluate to `false`, the function falls through and accepts NaN as in-range:

```java
public double requireNormalized(String fieldName, double value) {
    if (value < 0.0 || value > 1.0) {
        throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
    }
    return value;
}
```

Wait — re-reading: NaN passes *both* comparisons as false, so the throw is skipped and NaN is **returned silently**, same as `clamp`. So `requireNormalized` does NOT actually reject NaN. This contradicts the test class comment at line 10–12 ("the strict-validation cousin of AnalyticsScoreUtils that throws instead of clamping") and the production guardrail role it plays for `ValidationThresholds` (`lib-validation/config/ValidationThresholds.java:85-95`).

**Why it matters:** `ValidationThresholds` is loaded from config; a typo producing NaN in YAML deserialization would slip through the validator and turn the entire validation engine into a NaN propagator. This is the validator-of-last-resort and should be the strict line.

**Proposed fix:** Add an explicit NaN check: `if (Double.isNaN(value) || value < 0.0 || value > 1.0) throw ...`. Update the error message to mention NaN. Add a test asserting `requireNormalized("x", Double.NaN)` throws.

---

### 7. `requireNonNegative` silently accepts NaN and positive infinity

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\ValidationScoreUtils.java:18-22`

**Expected vs Actual:**

```java
public void requireNonNegative(String fieldName, double value) {
    if (value < 0.0) {
        throw new IllegalArgumentException(fieldName + " must be non-negative");
    }
}
```

NaN is accepted (same IEEE issue as Finding #6). `+Infinity` is accepted. Both then flow into weight totals in `DisequilibriumValidationEngine.weightedAverage(...)`.

**Why it matters:** A weight of `+Infinity` in `weightedAverage` makes the weighted total Infinity and the denominator Infinity, returning `NaN`. Same poisoning concern as Finding #1.

**Proposed fix:** `if (Double.isNaN(value) || value < 0.0 || Double.isInfinite(value)) throw ...`. Alternatively, since this is "weight" semantics, just `if (!(value >= 0.0 && Double.isFinite(value))) throw ...`.

---

### 8. `weightedAverage` returns 0.0 for a zero denominator without distinguishing "no data" from "all zeros"

**Severity:** Low
**Files:** `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\ValidationScoreUtils.java:24-26`, `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\AnalyticsScoreUtils.java:25-27`

**Expected vs Actual:** Both implementations:

```java
public double weightedAverage(double weightedTotal, double totalWeight) {
    return totalWeight == 0.0 ? 0.0 : weightedTotal / totalWeight;
}
```

Returning `0.0` for "no weights at all" conflates "we have zero evidence" with "we have evidence that everything is 0.0 confidence." In a probabilistic-state platform that drives PASS / WATCH / REJECT verdicts, this distinction matters: an empty input should arguably not pass the same threshold logic as a unanimously-zero one.

**Why it matters:** `DisequilibriumValidationEngine` (line 88) calls this on each verdict. If no weights are configured for a profile (a misconfiguration), the engine silently returns 0.0 → likely REJECT (looks like a legit low-confidence verdict) instead of surfacing the config error. Hard to debug.

**Proposed fix:** Either (a) throw `IllegalStateException("weightedAverage: totalWeight == 0")` and let callers explicitly handle the empty case, or (b) return `Double.NaN` (callers already need a NaN-safe guard per Finding #1), or (c) leave behavior but document the rationale in the javadoc. Add an `@throws` (or `@return 0.0 when ...`) javadoc.

---

### 9. Tests use `assertEquals(double, double)` without delta for exact comparisons that depend on FP equality

**Severity:** Low
**Files:** `C:\repos\red-dragon\red-dragon\lib-math\src\test\java\dev\reddragon\math\AnalyticsScoreUtilsTest.java:32`, `:38`, `:43`; `C:\repos\red-dragon\red-dragon\lib-math\src\test\java\dev\reddragon\math\MarketMathUtilsTest.java:22`, `:33`; `C:\repos\red-dragon\red-dragon\lib-math\src\test\java\dev\reddragon\math\ValidationScoreUtilsTest.java:18-20`, `:56`, `:61`

**Expected vs Actual:** JUnit 5 has both `assertEquals(double expected, double actual)` (which uses `Double.compare` equality) and `assertEquals(double expected, double actual, double delta)`. Most tests in this module mix the two:

```java
assertEquals(0.5, AnalyticsScoreUtils.average(0.0, 1.0));            // no delta
assertEquals(0.3, AnalyticsScoreUtils.average(0.2, 0.4), 1e-9);      // delta
```

```java
assertEquals(50.0, MarketMathUtils.average(0.0, 100.0));             // no delta
assertEquals(0.10, MarketMathUtils.safePercentChange(110.0, 100.0), 1e-9);
```

The exact-equality cases happen to work on current JVMs but make the tests brittle to refactors (e.g. switching `(left + right) / 2.0` to `left/2.0 + right/2.0` would break them).

**Why it matters:** Low. Tests are correct today. But the inconsistency signals the author wasn't sure of the rule, and that confusion will be inherited by anyone copying these as a template for new tests.

**Proposed fix:** Use the `delta` overload everywhere a double is asserted, with `1e-9` as the standard tolerance (already established by the existing usages). Or, if exact equality is genuinely required (e.g. for the clamp tests where the function returns one of the bound constants), add a comment.

---

### 10. No test covers `requireNormalized` returning the input value

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-math\src\test\java\dev\reddragon\math\ValidationScoreUtilsTest.java:17-21`

**Expected vs Actual:** `requireNormalized` is declared to return `double`, and callers in `ValidationThresholds.java` could chain it (currently they don't — they call it for the throw side effect only). The "accepts values in range" test does assert returns, so this is covered. **No issue here on second read.** Striking this finding.

(Verified: `assertEquals(0.5, ValidationScoreUtils.requireNormalized("score", 0.5))` does check the return.)

---

### 11. No test for `requireNormalized` / `requireNonNegative` with `Double.NaN` or `Double.POSITIVE_INFINITY`

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-math\src\test\java\dev\reddragon\math\ValidationScoreUtilsTest.java`

**Expected vs Actual:** The test file's javadoc (lines 10–13) describes these as "the strict-validation cousin of AnalyticsScoreUtils that throws instead of clamping," yet there is no test asserting strictness on NaN or Infinity inputs. This is what made Findings #6 and #7 latent.

**Why it matters:** Tests function as executable documentation of the contract. Their silence on NaN suggests the team has not considered the case. Adding tests will either confirm the current behavior is intentional (and the production guardrail is weaker than implied) or force a fix.

**Proposed fix:** After resolving Findings #6 and #7, add:

```java
@Test
void requireNormalizedRejectsNaN() {
    assertThrows(IllegalArgumentException.class,
        () -> ValidationScoreUtils.requireNormalized("score", Double.NaN));
}

@Test
void requireNonNegativeRejectsNaN() {
    assertThrows(IllegalArgumentException.class,
        () -> ValidationScoreUtils.requireNonNegative("weight", Double.NaN));
}
```

---

### 12. `package-info.java` description is narrower than the parent README

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-math\src\main\java\dev\reddragon\math\package-info.java:1-4`

**Expected vs Actual:** Parent README: "clamping, normalized-score validation, weighted averages, and percent-change math." Package-info:

```java
/**
 * Shared numeric helpers for score normalization and simple market math.
 */
```

This omits explicit mention of "clamping," "validation" (the throwing variants), and "weighted averages." Combined with Finding #5 (module pom description drift) this is the third place where lib-math's purpose statement disagrees with itself.

**Proposed fix:** Replace with the parent-README sentence verbatim.

---

## Strengths

1. **Determinism / purity is clean.** No I/O, no static mutable state, no threading, no `System.currentTimeMillis()`, no `Random`. All three classes are pure functions. This satisfies the ARCHITECTURE.md guarantee that lib-math is upstream of `lib-analytics`, which itself must be deterministic.

2. **Lombok `@UtilityClass` is used correctly.** It generates the private constructor and makes all members static, exactly the use case Lombok intends. This matches the README convention "Lombok is the default."

3. **No nested classes.** Every public type is a top-level file. Three utility classes, three files. The "no nested classes" rule from ARCHITECTURE.md is honored.

4. **Each public method has direct test coverage.** All seven public methods (`clamp` x2, `average` x2, `weightedAverage` x2, `safePercentChange`, `requireNormalized`, `requireNonNegative`) have at least one happy-path test, and most have at least one boundary/error test. Coverage is not deep (see Finding #11) but no method is untested.

5. **Zero unnecessary dependencies.** The module pom imports only `lombok` (provided) and `junit-jupiter` (test). No transitive Spring, no Guava, no Apache Commons Math creep. This is exactly what a "small shared helper" module should look like.

6. **`safePercentChange` correctly returns a fraction (`0.10` for a 10% move), not a percent (`10.0`).** Tests assert this, and the sole production caller (`MarketFeatureCalculator.safePercentChange`) consumes it as a fraction. Unit convention is internally consistent.

7. **Domain ranges are consistently `[0.0, 1.0]`** across `clamp`, `requireNormalized`, and downstream `MarketMathUtils.clamp` callers in `lib-domain/models/Market*Snapshot.java`. No mix of `0..100` style. This matches the "Each scorer takes a snapshot in and returns a 0.0–1.0 score out" rule from ARCHITECTURE.md line 137.

8. **`ValidationScoreUtils.requireNormalized` includes the field name in its error message,** and the test asserts it. This makes config-validation failures from `ValidationThresholds.java` debuggable without a stack-walking step.

9. **No drift beyond the documented purpose.** Every public method maps to one of the four documented pillars: clamping, normalized-score validation, weighted averages, percent-change math. No surprise additions like statistical helpers, random number generation, or date math.

10. **Integer overflow not a concern** because every operation is on `double`. There is no integer-window math, no `int` accumulator, no off-by-one window code in this module.

---

## Summary table — issue count by severity

| Severity | Count | Findings |
| --- | --- | --- |
| Critical | 0 | — |
| High | 1 | #1 (`clamp(NaN)` leaks NaN) |
| Medium | 2 | #2 (duplicate `clamp`/`average`/`weightedAverage`), #3 (`safePercentChange` denominator edge cases) |
| Low | 8 | #4, #5, #6, #7, #8, #9, #11, #12 |
| **Total actionable** | **11** | (#10 withdrawn after re-reading the test) |

The module is small, pure, well-tested at the happy-path level, and faithful to its documented charter. The biggest single risk is **NaN propagation through `clamp` and `requireNormalized`** (Findings #1 and #6 together): the module is positioned in ARCHITECTURE.md as the "no second line of defense" guardrail for normalized scores, but it silently passes `Double.NaN` through both the clamping and the validating entry points. Tightening those two checks plus consolidating the duplicate `clamp` implementations (Finding #2) would close ~80% of the audit's exposure without touching call sites.
