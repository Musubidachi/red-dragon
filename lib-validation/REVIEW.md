# lib-validation Review

Last updated: 2026-05-26

This review summarizes the current validation module after the local fix pass.
Current design decisions and test gaps are listed before compressed historical
fixes.

## Current Open Work

| Priority | Issue | Status | Impact | Next action |
| --- | --- | --- | --- | --- |
| High | Gate/scoring order | Open | The engine still computes factors and aggregate score before hard-gate results, while docs describe gates as preceding scoring. | Decide whether to short-circuit on gates or update docs to say score and gates are computed for audit and gates dominate the verdict. |
| High | Concentration input thresholds reuse `passThreshold` | Open | Raising the aggregate pass threshold also tightens asymmetry and earlyness requirements for concentrated tier. | Add dedicated concentration input thresholds. |
| High | Deployment confidence for STANDARD/PROBE | Open | `deploymentConfidenceScore` only constrains concentrated tier; weak confidence can still receive STANDARD when aggregate score passes. | Decide whether confidence gates every capital tier or is intentionally top-tier only. |
| Medium | Engine subservice injection | Open | `DisequilibriumValidationEngine` still constructs subservices manually from thresholds. | Switch to constructor-injected subservices if profile wiring needs shared beans or easier tests. |
| Medium | Threshold configuration binding | Open | Profile docs imply YAML-driven tuning, but local profiles still use hardcoded factory defaults. | Add `@ConfigurationProperties` binding or clarify that app-level beans own overrides. |
| Low | `HARD_GATE_FAILED` marker fragility | Open | Marker logic depends on every entry in the shared list being a failure. | Track failures separately or have checks return booleans. |
| Low | Threshold-profile tests | Open | Named profiles lack direct iteration tests for invariants and total weights. | Add a `ValidationThresholdProfileFactoryTest`. |

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
