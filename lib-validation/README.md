# lib-validation

> **MD layer:** owns the hard-gate half of L3 (Structural Validation) and the
> deployment-tier decision in L5. See [ARCHITECTURE.md](../ARCHITECTURE.md).

`lib-validation` turns candidate context and analytical signals into an
actionable review verdict.

## Responsibilities

* Apply hard rejection rules.
* Aggregate analytics outputs into a final candidate verdict.
* Produce human-readable reasons and risk flags.
* Separate required-data checks from trade-quality checks.
* Resolve a deployment posture separate from the verdict.
* Keep verdict logic deterministic and auditable.

## Verdicts And Tiers

Verdicts:

* `PASS` - candidate is strong enough for trader review.
* `WATCH` - candidate has promise, but needs confirmation or has moderate defects.
* `REJECT` - candidate fails one or more required validation rules.

Deployment tiers:

* `NONE`
* `OBSERVE`
* `PROBE`
* `STANDARD`
* `CONCENTRATED`

Older design notes may use names like `PASS_PROBE` or `PASS_CONCENTRATED`.
In current code, those are represented as `Verdict.PASS` plus the appropriate
`DeploymentTier`.

## Non-Responsibilities

This library does not pull raw candidates, fetch market data directly, perform
low-level indicator calculations, persist verdicts directly, place orders, size
positions, or replace trader judgment.

## Current Flow

```text
candidate + market-data snapshot + analytics snapshot
    -> CandidateValidationInput
    -> HardGateEvaluator
    -> ValidationFactorFactory
    -> weighted score
    -> VerdictResolver
    -> DeploymentResolver
    -> ValidationResult / ValidationAudit
```

## Current Package Layout

```text
lib-validation/src/main/java/dev/reddragon/validation
    services/         ValidationService facade
    services/engine/  hard gates, scoring, verdict and deployment resolution
    services/format/  display-ready summaries
    config/           thresholds and profiles
```

Verdict, DeploymentTier, ValidationResult, ValidationFactor, ValidationAudit,
ValidationStage, ValidationSummary, VerdictDecision, ReasonCode, and RiskFlag
all live in `lib-domain` so they can flow across modules. Numeric helpers
(clamping, weighted averages, score validation) live in `lib-math`. This
module owns only the validation engine and its configuration.

## Implemented Pieces

* `ValidationService`
* `DisequilibriumValidationEngine`
* `HardGateEvaluator`
* `ValidationFactorFactory`
* `ValidationConfidenceScorer`
* `VerdictResolver`
* `DeploymentResolver`
* `RiskFlagResolver`
* `ValidationSummaryFormatter`
* Threshold profiles: `STANDARD`, `CONSERVATIVE`, `AGGRESSIVE`,
  `CONCENTRATION_REVIEW`
* Bindable STANDARD threshold overrides via `ValidationThresholdProperties`

## Threshold Semantics

`ValidationThresholdProperties` binds `red-dragon.validation.*` overrides and
falls back to the STANDARD defaults when keys are omitted.

Aggregate thresholds decide the verdict and score tier:

* `watch-threshold`
* `pass-threshold`
* `observe-deployment-threshold`
* `probe-deployment-threshold`
* `standard-deployment-threshold`
* `concentration-threshold`

Input thresholds gate deployment posture after the aggregate score qualifies:

* `concentration-deployment-confidence-threshold`
* `concentration-asymmetry-threshold`
* `concentration-earlyness-threshold`
* `standard-deployment-confidence-threshold`
* `probe-deployment-confidence-threshold`

This keeps aggregate pass tuning from accidentally tightening concentration
earlyness/asymmetry gates. STANDARD and PROBE are actionable only when
`deploymentConfidenceScore` clears the matching confidence floor; otherwise a
qualified score downgrades to OBSERVE or NONE.

## Still Missing Compared To The Design Doc

* Account and instrument compatibility gates.
* Options-chain existence checks.
* Full manual-factor capture for propagation stage and narrative coherence.
* Fundamentals-backed materiality scoring.
* A dedicated nested `ValidationVerdict` contract exactly matching
  [VALIDATION_FRAMEWORK.md](VALIDATION_FRAMEWORK.md).

## Testing Expectations

Tests should cover missing required data, hard-gate behavior, watch/pass
threshold boundaries, deployment-tier boundaries, reason-code ordering, and
deterministic verdicts for fixed candidate snapshots.
