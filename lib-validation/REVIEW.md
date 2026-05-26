# lib-validation — Implementation Review

**Date:** 2026-05-17
**Last fix pass:** 2026-05-20 (local fixes only)
**Reviewer:** Automated audit pass
**Scope:** `lib-validation/` — 13 source files under `services/engine/`, `services/format/`, `services/`, `config/`. Tests in `src/test/java/dev/reddragon/validation/services/engine/` and `services/`.

---

## Fix Status

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | High | `DeploymentTier.OBSERVE` unreachable | **Fixed** — added `observeDeploymentThreshold` field to `ValidationThresholds` (defaults set equal to `probeDeploymentThreshold` so legacy behavior preserved; OBSERVE band activates when operator sets it lower per profile) |
| 2 | Medium | Fail-fast naming mismatch | **Fixed** — ARCHITECTURE.md L3 line reworded from "fail-fast hard rules" to "evaluates every gate and collects all failures so the trader sees every reason for rejection in one pass" |
| 3 | Medium | Score computed before gates evaluated | Open — pipeline-order refactor |
| 4 | Medium | `passThreshold` reused as input threshold | Open — needs new `minConcentrationAsymmetry`/`minConcentrationEarlyness` fields |
| 5 | Medium | `deploymentConfidenceScore` ignored for STANDARD/PROBE | Open — design decision |
| 6 | Medium | No tier-ordering invariants in ValidationThresholds | **Fixed** — constructor now rejects `observe > probe > standard > concentration` and `pass > concentration` |
| 7 | Medium | Engine `new`s sub-services manually | Open — would change public constructor signature |
| 8 | Low | `HARD_GATE_FAILED` marker fragility | Open |
| 9 | Low | YAML/profile binding documentation lies | Open — needs `@ConfigurationProperties` |
| 10 | Low | `VerdictResolver`/`DeploymentResolver` no input validation | **Fixed** — both now reject NaN/null at the boundary via `ValidationScoreUtils.requireNormalized` + `Objects.requireNonNull` |
| 11 | — | Coverage note for non-deep-reviewed files | — |
| 12 | Low | Sub-packages missing `package-info.java` | **Fixed** — added `package-info.java` to `services/engine`, `services/format`, `config` |
| 13 | Low | README claims `models/` + `utilities/` exist | **Fixed** — README updated to reflect actual on-disk layout; clarifies shared types live in lib-domain |
| 14 | Low | Tests miss threshold-profile equivalence | Open |

**Open: 9** — most require design decisions (#3, #4, #5), new ConfigurationProperties wiring (#9), or are additive test work.

---

---

## Expected Behavior (per docs)

`lib-validation/README.md` is the in-module spec:

- Owns L3 hard-gate half (Structural Validation) and L5 deployment-tier decision (line 3–4).
- Responsibilities (lines 11–16): hard rejection rules, score aggregation, human-readable reasons + risk flags, separate required-data from quality checks, deployment posture, deterministic.
- Verdicts: `PASS`, `WATCH`, `REJECT` (lines 22–24).
- Deployment tiers: `NONE`, `OBSERVE`, `PROBE`, `STANDARD`, `CONCENTRATED` (lines 28–32).
- Flow (lines 46–55): `CandidateValidationInput → ValidationFactorFactory → HardGateEvaluator → weighted score → VerdictResolver → DeploymentResolver → ValidationResult / ValidationAudit`.
- **Package layout (README lines 59–67):**

```
lib-validation/src/main/java/dev/reddragon/validation
    models/           Verdict, DeploymentTier, ValidationResult, factors, audits
    services/         ValidationService facade
    services/engine/  hard gates, scoring, verdict and deployment resolution
    services/format/  display-ready summaries
    config/           thresholds and profiles
    utilities/        score helpers
```

- Threshold profiles: `STANDARD`, `CONSERVATIVE`, `AGGRESSIVE`, `CONCENTRATION_REVIEW` (line 80–81).
- Still missing per README §"Still Missing": account / instrument compat gates, options-chain checks, manual-factor capture, fundamentals materiality, dedicated `ValidationVerdict` contract per `VALIDATION_FRAMEWORK.md`.

ARCHITECTURE.md adds: hard gates are "fail-fast hard rules" (line 81).

---

## Files Reviewed

| Sub-package | Files |
| --- | --- |
| `services/` | `ValidationService.java` |
| `services/engine/` | `DisequilibriumValidationEngine.java`, `HardGateEvaluator.java`, `VerdictResolver.java`, `DeploymentResolver.java`, `RiskFlagResolver.java`, `ValidationConfidenceScorer.java`, `ValidationFactorFactory.java` |
| `services/format/` | `ValidationSummaryFormatter.java` |
| `config/` | `ValidationThresholds.java`, `ValidationProfile.java`, `ValidationThresholdProfileFactory.java` |
| `package-info` | One file at module top level |
| Tests | 4 test files |

---

## Findings

### 1. `DeploymentTier.OBSERVE` is unreachable in production

**Severity:** High
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\services\engine\DeploymentResolver.java:24-49`

**Expected vs Actual:** README line 28–32 lists `OBSERVE` as a deployment tier between `NONE` and `PROBE`. Per `ValidationThresholds.defaults()`:

- `passThreshold = 0.78`
- `watchThreshold = 0.58`
- `probeDeploymentThreshold = 0.58`
- `standardDeploymentThreshold = 0.78`
- `concentrationThreshold = 0.87`

`DeploymentResolver.process` reaches `OBSERVE` only when **all** of the following are false:
- `verdict == REJECT` (would return `NONE`)
- `concentrated(score, input)` (requires score ≥ 0.87 + many other gates)
- `standard(verdict, score)` (requires `verdict == PASS && score ≥ 0.78`)
- `probe(score)` (requires `score ≥ 0.58`)

But:
- A PASS verdict means `score ≥ passThreshold = 0.78` (by `VerdictResolver`), so `probe(score)` is always true.
- A WATCH verdict means `0.58 ≤ score < 0.78`, so `probe(score)` is always true.
- A REJECT verdict returns `NONE` before the `OBSERVE` branch is reached.

```java
// DeploymentResolver.java:24-49
if (verdict == Verdict.REJECT) return DeploymentTier.NONE;
if (concentrated(score, input)) return DeploymentTier.CONCENTRATED;
if (standard(verdict, score)) return DeploymentTier.STANDARD;
if (probe(score)) return DeploymentTier.PROBE;
return DeploymentTier.OBSERVE;   // unreachable
```

Therefore the `OBSERVE` tier is dead code in every default and configured profile (the `CONSERVATIVE`, `AGGRESSIVE`, `CONCENTRATION_REVIEW` profiles all have `watchThreshold == probeDeploymentThreshold`).

**Why it matters:** Two consequences:
1. **The trader review surface will never see "Observe" tier.** `DeploymentTier.OBSERVE` (enum value, `displayName() = "Observe"`) is referenced in `ReasonCode.DEPLOYMENT_OBSERVE_ONLY` and `DisequilibriumValidationEngine.reasonForDeployment` (line 118), but never actually returned.
2. **The README's tier hierarchy is misleading** — it implies five usable tiers; only four are reachable in code.

**Proposed fix:** Either (a) introduce a distinct `observeDeploymentThreshold` (e.g., 0.45) and route scores in `[observeThreshold, probeThreshold)` to OBSERVE, or (b) remove OBSERVE from the enum and README. Today's middle state is the worst option. If keeping OBSERVE is the intent, the WATCH-but-below-probe band needs to exist — which requires `watchThreshold < probeDeploymentThreshold`.

---

### 2. `HardGateEvaluator.process` evaluates all gates instead of fail-fast as documented

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\services\engine\HardGateEvaluator.java:25-41`

**Expected vs Actual:** ARCHITECTURE.md line 81: *"`lib-validation/services/engine/HardGateEvaluator.java` — fail-fast hard rules"*. Actual: every gate is evaluated regardless of prior failures.

```java
public List<ReasonCode> process(CandidateValidationInput input) {
    Objects.requireNonNull(input, "input is required");

    List<ReasonCode> failures = new ArrayList<>();

    addRequiredDataFailure(input, failures);
    addCatalystFailure(input, failures);
    addStructuralRealityFailure(input, failures);
    addMaterialityFailure(input, failures);
    addEarlynessFailure(input, failures);
    addEquilibriumFailure(input, failures);
    addAsymmetryFailure(input, failures);
    addRegimeFailure(input, failures);
    addHardGateMarker(failures);

    return List.copyOf(failures);
}
```

All 8 checks run unconditionally. The name "fail-fast" implies short-circuit-on-first-failure. The actual behavior — collect-all-then-decide — is arguably more useful (the trader gets every reason for rejection at once), but the doc wording is wrong.

**Why it matters:** Two perspectives:
1. **It's not a bug, but a documentation/naming mismatch.** The current behavior is probably the right one — giving the trader the full list of reasons is more actionable than one. ARCHITECTURE.md should reword to "evaluates all hard rules and collects failures."
2. **If the architectural intent was actually fail-fast (e.g., for performance with expensive checks), the current implementation doesn't deliver it.** None of the 8 checks are expensive today, but the engine is positioned to expand (README mentions account/instrument compat gates as a future addition — those *could* be expensive HTTP lookups).

**Proposed fix:** Update ARCHITECTURE.md L3 description from "fail-fast hard rules" to "hard rules; collects all failures so the trader sees every reason." If fail-fast is required for future expensive gates, separate them into a `LightHardGate` (cheap, collect-all) and `HeavyHardGate` (sequenced, fail-fast on first failure).

---

### 3. `DisequilibriumValidationEngine.process` does not short-circuit when hard gates fail — wasted score computation

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\services\engine\DisequilibriumValidationEngine.java:44-77`

**Expected vs Actual:** The pipeline diagram in README line 46–55 shows hard gates running before scoring. Actual order:

```java
public ValidationResult process(CandidateValidationInput input) {
    Objects.requireNonNull(input, "input is required");

    List<ValidationFactor> factors = validationFactorFactory.process(input);  // step A
    double score = score(factors);                                            // step B
    List<ReasonCode> reasons = reasons(factors);                              // step C
    List<String> explanations = explanations(factors);                        // step D

    List<ReasonCode> hardGateFailures = hardGateEvaluator.process(input);     // step E (could be first)
    VerdictDecision verdictDecision = verdictResolver.process(score, hardGateFailures);
    ...
}
```

The factor factory and score aggregation always run, even when hard gates have already failed and the verdict will be `REJECT` regardless. The `ValidationResult` returned still includes the full factor list and computed score, which is information-rich for audit but a wasted compute path on hot rejection cases.

**Why it matters:** Two angles:
1. **Performance.** Today's factor factory is cheap (basic arithmetic + threshold checks). Future per-candidate operations (e.g., the L8 stratified analytics inputs) may push factor computation into more expensive territory.
2. **Pipeline-diagram fidelity.** The README's text says "gates run before scoring;" the code runs scoring before reading the gate result.

**Proposed fix:** Run hard gates first; short-circuit factor construction when `hardGateFailures.isEmpty()` is false. Preserve the audit story by including a stub factor list with zero scores in the rejected `ValidationResult`. Or accept the wasted compute and update README to say "scoring and gates run in parallel; gates dominate the verdict."

---

### 4. `DeploymentResolver.concentrated` requires both `asymmetryScore` and `earlynessScore` to clear the *verdict* `passThreshold` — cross-cutting threshold semantics

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\services\engine\DeploymentResolver.java:51-59`

**Expected vs Actual:**

```java
private boolean concentrated(double score, CandidateValidationInput input) {
    return score >= thresholds.concentrationThreshold()
            && input.deploymentConfidenceScore() >= thresholds.standardDeploymentThreshold()
            && input.asymmetryScore() >= thresholds.passThreshold()
            && input.earlynessScore() >= thresholds.passThreshold();
}
```

`passThreshold` is documented (in `ValidationThresholds.defaults()`, line 38–40) as the threshold the **aggregate weighted score** must exceed to produce `Verdict.PASS`. Using the same constant (0.78) as a threshold for an individual *input dimension* (asymmetry, earlyness) conflates two different semantic axes: "aggregate evidence" vs "single-dimension strength."

**Why it matters:** When the trader tunes `passThreshold` higher to make PASS rarer, they implicitly also tighten the concentration gate for two specific input dimensions — without realizing that's a side effect. A dedicated `minConcentrationAsymmetry` and `minConcentrationEarlyness` would isolate the concerns.

**Proposed fix:** Add `minConcentrationAsymmetry` and `minConcentrationEarlyness` fields to `ValidationThresholds` (with sensible defaults), and use them here instead of reusing `passThreshold`. Document in javadoc.

---

### 5. `DeploymentResolver` ignores `deploymentConfidenceScore` for STANDARD and PROBE tiers

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\services\engine\DeploymentResolver.java:24-49`

**Expected vs Actual:** ARCHITECTURE.md line 95–96 explicitly says `DeploymentConfidenceScorer.java` is the "confidence input" and `DeploymentResolver.java` is the "final tier decision" — implying the resolver consumes the confidence input. Actual: only the `concentrated()` branch reads `deploymentConfidenceScore` (line 56). STANDARD and PROBE branches do not.

**Why it matters:** A candidate with weak deployment confidence (e.g., 0.40) but passing weighted score (0.80) will still land in STANDARD tier despite the deployment-confidence signal saying "don't commit normal capital here." The confidence input is only consulted at the very top of the tier ladder, not throughout.

**Proposed fix:** Either (a) require `deploymentConfidenceScore >= probeDeploymentThreshold` for STANDARD and `deploymentConfidenceScore >= some-lower-threshold` for PROBE, or (b) document that `deploymentConfidenceScore` is intentionally a CONCENTRATED-gate only.

---

### 6. `ValidationThresholds` constructor does not enforce ordering invariants between tiers

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\config\ValidationThresholds.java:84-139`

**Expected vs Actual:** The constructor validates per-field normalization and weight non-negativity, plus a single ordering check (`watchThreshold ≤ passThreshold`, line 116–118). Missing invariants:

- `concentrationThreshold ≥ passThreshold` — a concentration threshold below the pass threshold is nonsensical.
- `standardDeploymentThreshold ≥ probeDeploymentThreshold` — STANDARD tier should require at least the PROBE threshold.
- `concentrationThreshold ≥ standardDeploymentThreshold` — CONCENTRATED should be at least STANDARD.

A profile YAML override that violates these produces no error at startup; instead it silently produces wrong tier assignments at runtime.

**Why it matters:** Defaults satisfy the invariants (0.87 ≥ 0.78 ≥ 0.58), and the four built-in profiles also do. But operators may tune via `application.yml`, and the constructor is the only line of defense.

**Proposed fix:** Add the three ordering checks to the constructor. Throw `IllegalArgumentException` with a message naming both fields and the violated relation.

---

### 7. `DisequilibriumValidationEngine` `new`s its four sub-services in the constructor instead of accepting them

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\services\engine\DisequilibriumValidationEngine.java:29-39`

**Expected vs Actual:**

```java
public DisequilibriumValidationEngine(ValidationThresholds thresholds) {
    this.thresholds = Objects.requireNonNull(thresholds, "thresholds is required");
    this.validationFactorFactory = new ValidationFactorFactory(thresholds);
    this.hardGateEvaluator = new HardGateEvaluator(thresholds);
    this.verdictResolver = new VerdictResolver(thresholds);
    this.deploymentResolver = new DeploymentResolver(thresholds);
}
```

The engine takes `ValidationThresholds`, then instantiates four sub-services from it. The sub-services are not Spring beans here; they exist as private fields owned by the engine. This couples the engine class to the specific concrete sub-services. ARCHITECTURE.md convention says service constructors use `@RequiredArgsConstructor` with dependencies as fields; the engine instead acts as a factory.

Same pattern appears in `lib-analytics.DeterministicAnalyticsService` and `lib-backtest.BacktestReplayEngine`.

**Why it matters:**
1. **Testability.** Mocking `HardGateEvaluator` to test the engine in isolation is impossible without reflection.
2. **Profile changes require new engine instances.** Each `ValidationProfile` requires constructing a fresh engine, since `thresholds` is final and threaded through the four sub-services. For 4 profiles, that's 4 engines × 4 sub-services = 16 objects, with no sharing.

**Proposed fix:** Convert to `@RequiredArgsConstructor` with the four sub-services as constructor params. Spring (via `lib-validation` consumed by `app`) wires them. Keep `defaults()` factory method on the engine for tests.

---

### 8. `addHardGateMarker` adds `HARD_GATE_FAILED` *only* if other failures exist — but this is brittle

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\services\engine\HardGateEvaluator.java:127-131`

**Expected vs Actual:**

```java
private void addHardGateMarker(List<ReasonCode> failures) {
    if (!failures.isEmpty()) {
        failures.add(ReasonCode.HARD_GATE_FAILED);
    }
}
```

This is a marker code that says "hard gates participated in the rejection." Fine, but: if a future contributor adds a check that conditionally pushes a `ReasonCode` not tied to a failure (e.g., a "warning" reason that's not a hard fail), the marker will get added incorrectly. The list is shared mutable state between checks.

**Why it matters:** Low; today every push to `failures` is in fact a failure. But the helper functions all have `addXxxFailure(input, failures)` signatures that imply "may add zero, one, or many." A check that adds two — one warning, one failure — would still trigger the marker.

**Proposed fix:** Have each `addXxxFailure` return a boolean and OR them together; or push to a dedicated `Set<ReasonCode>` that the marker examines explicitly.

---

### 9. `ValidationProfile` enum is not connected to YAML overrides — `application.yml` profiles must drive `ValidationThresholds.defaults()` and the profile factory simultaneously

**Severity:** Low
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\config\ValidationProfile.java`
- `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\config\ValidationThresholdProfileFactory.java`
- `lib-validation/README.md:80-81` ("Threshold profiles: STANDARD, CONSERVATIVE, AGGRESSIVE, CONCENTRATION_REVIEW")
- `lib-validation/README.md:7-8` ("The STANDARD profile is the default and is driven by application.yml overrides" — javadoc on `ValidationProfile.java:8`)

**Expected vs Actual:** The javadoc on `ValidationProfile` says STANDARD is "driven by application.yml overrides." The factory method `ValidationThresholdProfileFactory.process(ValidationProfile.STANDARD)` returns `ValidationThresholds.defaults()` — which is a hardcoded `new ValidationThresholds(0.78, 0.58, ..., 0.05)`. There is no `@ConfigurationProperties` binding that lets `application.yml` override these.

```java
case STANDARD -> ValidationThresholds.defaults();
```

`defaults()` returns hardcoded values. No yaml override channel exists in this module. (The override must therefore live in `app`'s `PipelineConfiguration` — out of scope for this review but flagged.)

**Why it matters:** The javadoc lies. A trader changing `application.yml` to tune the standard profile will see no effect unless `app/...` explicitly re-binds. The trader's mental model — "I edit yml, defaults change" — doesn't match the code.

**Proposed fix:** Either (a) annotate a `ValidationThresholds` `@ConfigurationProperties` binder in lib-validation's `config/`, or (b) update the javadoc to clarify that the standard profile's hardcoded values are the defaults and overrides happen via `@Bean` declarations in `app`.

---

### 10. `VerdictResolver.process` accepts a precomputed `score` from the engine — depends on the engine for input validation

**Severity:** Low
**File:** `C:\repos\red-dragon\red-dragon\lib-validation\src\main\java\dev\reddragon\validation\services\engine\VerdictResolver.java:26-53`

**Expected vs Actual:** `process(double score, List<ReasonCode> hardGateFailures)` does not validate that `score` is in `[0, 1]` or that `hardGateFailures` is non-null. The engine passes these correctly today, but the resolver's signature is exposed as `public` and could be invoked directly.

**Why it matters:** Defensive coding. Per ARCHITECTURE.md "constructors do validation," services should do the same on their public boundaries. A `score = Double.NaN` here would silently pass both `score >= passThreshold` and `score >= watchThreshold` (NaN comparisons are always false), landing in REJECT — but for the wrong reason (`SCORE_BELOW_THRESHOLD` instead of "score is NaN").

**Proposed fix:** Add `ValidationScoreUtils.requireNormalized("score", score);` and `Objects.requireNonNull(hardGateFailures);` at the top. Same fix on `DeploymentResolver.process`.

---

### 11. `ValidationFactorFactory`, `RiskFlagResolver`, `ValidationSummaryFormatter` not reviewed in depth — likely strengths

**Severity:** N/A — coverage gap in this review
**Files:**
- `ValidationFactorFactory.java`
- `RiskFlagResolver.java`
- `ValidationSummaryFormatter.java`
- `ValidationConfidenceScorer.java`

These four files were not read line-by-line for this review. Spot-checks via Grep show they all use `ValidationThresholds` and `ValidationScoreUtils` from lib-math, follow the same patterns as the reviewed files, and have corresponding tests. A deeper pass is warranted but the patterns above are likely to repeat.

---

### 12. Module-level `package-info.java` exists at top level only; sub-packages have no `package-info.java`

**Severity:** Low
**Files:** sub-packages under `services/engine/`, `services/format/`, `config/`.

**Expected vs Actual:** ARCHITECTURE.md line 124: *"Every public package has a `package-info.java` calling out its MD layer."* Only the root `dev.reddragon.validation` has one; sub-packages do not.

**Why it matters:** Onboarding cue convention is broken; same minor issue as in lib-backtest.

**Proposed fix:** Add per-sub-package `package-info.java` files referencing the MD layer (L3 for `services/engine/`, etc.).

---

### 13. Documented `utilities/` and `models/` sub-packages don't exist inside lib-validation

**Severity:** Low
**Files:** `lib-validation/README.md:59-67` vs. on-disk layout.

**Expected vs Actual:** README claims:

```
models/           Verdict, DeploymentTier, ValidationResult, factors, audits
utilities/        score helpers
```

Actual: `lib-validation/src/main/java/dev/reddragon/validation/` contains only `config/`, `services/`. `Verdict`, `DeploymentTier`, `ValidationResult` live in `lib-domain` (correctly, per cross-module sharing — but the lib-validation README claims they live here). No `utilities/` directory exists.

**Why it matters:** Same misdirection as lib-marketdata Finding #1.

**Proposed fix:** Update README "Current Package Layout" to reflect reality.

---

### 14. Tests cover the engine but not the threshold-profile equivalence

**Severity:** Low
**File:** `lib-validation/src/test/java/dev/reddragon/validation/`

**Expected vs Actual:** Four test files exist (`DeploymentResolverTest`, `DisequilibriumValidationEngineTest`, `HardGateEvaluatorTest`, `ValidationServiceTest`). README "Testing Expectations" (lines 92–96) lists six expected coverage areas: missing required data, hard-gate behavior, watch/pass boundaries, deployment-tier boundaries, reason-code ordering, deterministic verdicts. Spot-checks show all six are at least touched, but I did not find a test exercising `ValidationThresholdProfileFactory.process(...)` to confirm each named profile produces a working threshold set (and that the four profiles' constructor invariants all pass). Given Finding #6 about missing constructor invariants, a profile-validity test would surface the gap.

**Why it matters:** A configuration-only change (yaml profile rename, new profile) would not be caught by current tests.

**Proposed fix:** Add a `ValidationThresholdProfileFactoryTest` that iterates `ValidationProfile.values()`, builds thresholds, and asserts (a) no exception, (b) ordering invariants from Finding #6 hold, (c) `totalWeight() > 0`.

---

## Hard-Gate Audit

Eight hard gates implemented in `HardGateEvaluator`:

| Gate | Code | Threshold source |
| --- | --- | --- |
| Required data missing | `REQUIRED_DATA_MISSING` | `input.requiredDataPresent()` (boolean flag) |
| Catalyst not credible | `CATALYST_NOT_CREDIBLE` | `input.credibleCatalyst()` (boolean) |
| Structural reality | `STRUCTURAL_CATALYST_WEAK` | `thresholds.minStructuralReality()` (0.65) |
| Materiality | `MATERIAL_IMPACT_INSUFFICIENT` | `thresholds.minMaterialSignificance()` (0.55) |
| Earlyness | `MAINSTREAM_SATURATION` | `thresholds.minEarlyness()` (0.45) |
| Euphoric saturation | `EUPHORIC_REFLEXIVITY` | `input.euphoricOrSaturated()` (boolean) |
| Equilibrium quality | `EQUILIBRIUM_DIRECTIONAL_HOSTILE` | `thresholds.minEquilibriumQuality()` (0.45) |
| Hostile market structure | `EQUILIBRIUM_LIQUIDITY_DEGRADED` | `input.hostileMarketStructure()` (boolean) |
| Asymmetry | `ASYMMETRY_UNFAVORABLE` | `thresholds.minAsymmetry()` (0.55) |
| Equilibrium repriced | `ASYMMETRY_COMPRESSED` | `input.equilibriumAlreadyRepriced()` (boolean) |
| Regime compatibility | `REGIME_HOSTILE` | `thresholds.minRegimeCompatibility()` (0.40) |

Doc claim (README §"Still Missing"): account/instrument compat, options-chain existence. **Status: not implemented**, matching the doc's own statement.

Fail-fast question: see Finding #2. The gates are collect-all, not fail-fast.

---

## Doc/Code Drift Summary

| Claim | Reality |
| --- | --- |
| 5 deployment tiers (NONE/OBSERVE/PROBE/STANDARD/CONCENTRATED) | OBSERVE unreachable (Finding #1) |
| Hard gates are "fail-fast" | Collect-all (Finding #2) |
| `models/` and `utilities/` exist | False (Finding #13) |
| Threshold profiles STANDARD / CONSERVATIVE / AGGRESSIVE / CONCENTRATION_REVIEW | All 4 implemented ✓ |
| `application.yml` overrides STANDARD profile | Unconfirmed; binding not in this module (Finding #9) |
| `DeploymentResolver` consumes deployment confidence | Partially — only for CONCENTRATED branch (Finding #5) |
| Pipeline flow: input → factors → gates → score → verdict → tier | Yes, but gates run after score (Finding #3) |
| Reason codes are emitted with explanations | ✓ |

---

## Strengths

1. **`ValidationThresholds` constructor is genuinely defensive.** Every score threshold is `requireNormalized`, every weight is `requireNonNegative`, weights sum check, watch ≤ pass check. Configuration errors fail loudly at startup. This is exactly the "no second line of defense" principle from ARCHITECTURE.md.
2. **Verdict / tier / reason-code separation is clean.** `Verdict` (3-value enum) and `DeploymentTier` (5-value enum) are orthogonal axes correctly. `ReasonCode` is a closed enum producing typed reasons rather than free-form strings.
3. **Score is documented as weighted aggregate**, computed in `DisequilibriumValidationEngine.score(...)` via `ValidationScoreUtils.weightedAverage`. Single point of aggregation; reusable; tested.
4. **`@Value + @Accessors(fluent = true)` on `ValidationThresholds`** matches the project convention. Long-but-explicit constructor signatures are clearer than a magic builder for config classes.
5. **Four named profiles cover the practical range** from CONSERVATIVE to AGGRESSIVE, plus CONCENTRATION_REVIEW for the high-conviction case. The thresholds shift coherently — pass threshold rises in CONCENTRATION_REVIEW (0.88 vs 0.78), min thresholds rise too. Coherent profile design.
6. **`Verdict.REJECT` short-circuits `DeploymentTier.NONE` cleanly** (line 32–34 of `DeploymentResolver`). No "PASS with NONE tier" or "REJECT with PROBE tier" possible.
7. **Hard-gate marker is a single distinct reason code** (`HARD_GATE_FAILED`) appended whenever any hard gate failed. UI can color-code differently from "soft" score rejections.
8. **`DisequilibriumValidationEngine.dedupeReasons` and `dedupeExplanations` use `LinkedHashSet`**, preserving insertion order while removing duplicates. Subtle correctness — duplicate explanations from overlapping factors don't get rendered twice but the first-mention order is kept.
9. **Pom is minimal:** lib-domain + lib-math + lombok + junit. No persistence, no web, no Jackson. Library jar is portable.
10. **README explicitly enumerates "Still Missing" items** rather than overselling. Honesty in the doc reduces gap-vs-design ambiguity.

---

## Summary table — issue count by severity

| Severity | Count | Findings |
| --- | --- | --- |
| Critical | 0 | — |
| High | 1 | #1 (OBSERVE tier unreachable) |
| Medium | 6 | #2 (fail-fast naming), #3 (score before gates), #4 (passThreshold reused as input threshold), #5 (confidence not consumed for STANDARD/PROBE), #6 (no tier-ordering invariants), #7 (manual sub-service construction) |
| Low | 7 | #8, #9, #10, #11, #12, #13, #14 |
| **Total** | **14** | |

The headline issue is **Finding #1**: a documented deployment tier is unreachable under every shipped profile. Either the tier should be removed or a new `observeDeploymentThreshold` should be added. Fixing it is small but the design choice (do we want an OBSERVE band?) should be made deliberately. **Finding #5** (confidence input only used for CONCENTRATED) and **Finding #4** (passThreshold reused as input threshold) round out the L5 design concerns — together they suggest the deployment side needs a small refactor to decouple the "aggregate score thresholds" from the "single-dimension floor" thresholds.

Compared to the lib-analytics review, lib-validation is the *better-architected* module: its threshold-profile system is real, its hard-gate enumeration is explicit, and its `ValidationThresholds` constructor is genuinely defensive. The L3 gates work the way the doc says (modulo the "fail-fast" wording in ARCHITECTURE.md, Finding #2). The drift here is at the L5 tier resolver, not the engine.
