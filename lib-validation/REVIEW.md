# lib-validation Review

Last updated: 2026-05-27

This review summarizes the current validation module after the local fix pass.
Current design decisions and test gaps are listed before compressed historical
fixes.

## Current Open Work

No module-local open work remains for RD-M7, RD-M8, RD-M9, RD-M10, RD-L4, or
RD-L5. Root tracking is intentionally left to the root issue index.

## Resolved In This Pass

| Issue | Status | Decision / fixed outcome |
| --- | --- | --- |
| RD-M7 Gate/scoring order | Fixed | Hard gates now run before factor scoring and dominate the final verdict. Factor scoring still runs after gates so rejected candidates keep audit context. |
| RD-M8 Concentration input thresholds | Fixed | Concentration no longer reuses aggregate `passThreshold` for input gates; it has dedicated deployment-confidence, asymmetry, and earlyness thresholds. |
| RD-M9 STANDARD/PROBE deployment confidence | Fixed | Deployment confidence gates every actionable tier. If aggregate score qualifies but confidence misses the tier floor, the resolver downgrades to the next non-blocked posture. |
| RD-M10 Threshold configuration binding | Fixed | `ValidationThresholdProperties` in `lib-validation` binds `red-dragon.validation.*`; the app wrapper delegates to it only to expose the Spring bean. |
| RD-L4 Hard-gate marker and subservice construction | Fixed | `HARD_GATE_FAILED` is based on explicit gate outcomes, and `DisequilibriumValidationEngine` supports direct subservice injection. |
| RD-L5 Validation profile tests | Fixed | Profile, binding, and deployment boundary tests now cover threshold ordering, weights, and confidence-gate semantics. |

## Implemented Surface

Implemented today:

* `ValidationService` facade.
* `DisequilibriumValidationEngine`.
* `ValidationFactorFactory`, `ValidationConfidenceScorer`, `HardGateEvaluator`,
  `VerdictResolver`, `DeploymentResolver`, and `RiskFlagResolver`.
* `ValidationSummaryFormatter`.
* Threshold profiles: `STANDARD`, `CONSERVATIVE`, `AGGRESSIVE`, and
  `CONCENTRATION_REVIEW`.
* Deployment tiers: `NONE`, `OBSERVE`, `PROBE`, `STANDARD`, and `CONCENTRATED`.

## Historical Fixes

| Area | Fixed outcome |
| --- | --- |
| OBSERVE reachability | `ValidationThresholds` gained an observe threshold, preserving legacy defaults while enabling an OBSERVE band. |
| Hard-gate documentation | Architecture wording now says gates collect all failures rather than fail fast. |
| Threshold invariants | Constructor ordering checks now reject invalid tier relationships. |
| Public resolver validation | Verdict and deployment resolvers validate normalized scores and null inputs. |
| Package docs | Subpackage `package-info.java` files were added. |
| README layout | Module README now reflects that shared validation models live in `lib-domain` and helpers in `lib-math`. |

## Still Missing Compared To The Design Doc

The module README correctly marks these as future work:

* account and instrument compatibility gates
* options-chain existence checks
* full manual-factor capture for propagation and narrative coherence
* fundamentals-backed materiality scoring
* a dedicated nested `ValidationVerdict` contract exactly matching
  `VALIDATION_FRAMEWORK.md`

Root tracking: [../ISSUES.md](../ISSUES.md).
