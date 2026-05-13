# lib-analytics

Pure-function scoring for the disequilibrium pipeline. Five of the eight MD
layers live here — every per-layer scorer is in its own sub-package so the
mapping is visible at a glance.

## MD-layer mapping

| Sub-package      | MD layer | Job                                                       |
| ---------------- | -------- | --------------------------------------------------------- |
| `structural/`    | L3       | Is the catalyst real, material, and meaningful?           |
| `classification/`| L4       | What market regime are we in?                             |
| `deployment/`    | L5       | How confidently should we deploy?                         |
| `propagation/`   | L6       | Is the narrative early or saturated?                      |
| `meta/`          | L8       | Is our own edge drifting?                                 |
| `service/`       | —        | Orchestrators that blend the above into one snapshot.     |
| `model/`         | —        | Shared value objects (snapshots, labels, breakdowns).     |
| `util/`          | —        | Pure math helpers.                                        |

Each sub-package has a `package-info.java` listing its members and what they do.

## Public API (the only classes other modules import)

* `DeterministicAnalyticsService` — the main pipeline entry. Takes a
  candidate + market-data snapshot and emits an `AnalyticsSnapshot`.
* `MarketStateClassifier` — standalone regime classifier exposed via
  the `/market-state` HTTP endpoint.
* `LongHorizonCalibrationAnalyzer` — meta-layer drift detection from a
  batch of realized trade outcomes.

Everything else is an internal implementation detail of those three classes.
The 18 per-layer scorers/analyzers in the sub-packages are package-private
in spirit even where they are technically `public`.

## Rules for this module

1. **Pure functions only.** No `I/O`, no static state, no time-of-day branching,
   no portfolio awareness. Same input ⇒ same output, always.
2. **Validation in the constructor.** Value objects use Lombok `@Value` and
   enforce normalized ranges in the explicit constructor. There is no second
   line of defense downstream.
3. **Every score includes its explanation.** Scorers don't return a bare
   `double` for free-floating consumption — they return a snapshot/finding
   with reason codes attached so review is auditable.
4. **No nested classes.** If you have a helper record or enum, it lives in
   its own top-level file in the same package.
5. **Adding a new scorer?** Put it in the layer sub-package that matches its
   job — never in `service/`. The orchestrator in `service/` is the only
   thing that crosses layers.

## Where each MD layer lives

### L3 — Structural Validation
* `AdversarialValidationAnalyzer` — contradiction-finding against the bullish thesis
* `AsymmetryScorer` — remaining risk/reward asymmetry
* `DilutionRiskScorer` — dilution / share-issuance risk
* `MaterialityImpactScorer` — catalyst materiality vs company size

### L4 — Market-State Classification
* `RegimeCompatibilityScorer` — top-level regime label
* `EquilibriumQualityScorer` — mean-reversion quality
* `EquilibriumPhaseAnalyzer` — intraday phase label
* `DirectionalPersistenceScorer` — trend persistence
* `VolatilityExpansionScorer` — realized-vol expansion
* `VwapInteractionScorer` — VWAP behavior
* `LiquidityTextureScorer` — depth, spread, participation
* `OptionsFlowScorer` — unusual options activity

### L5 — Deployment
* `DeploymentConfidenceScorer` — confidence input. Tier decision itself lives in `lib-validation`.

### L6 — Narrative Propagation
* `NarrativeExpansionScorer`
* `PropagationPhaseAnalyzer`
* `ReflexivityScorer`
* `SectorPropagationScorer`

### L8 — Meta-System Adaptation
* `LiveContextAdaptationAnalyzer`
* `LongHorizonCalibrationAnalyzer`

## Testing expectations

* Deterministic scoring for fixed inputs
* Boundary behavior around thresholds
* Missing or incomplete snapshots
* Reason-code generation
* Aggregation math in the orchestrator

Don't test analytics by depending on live market data. Feed fixed snapshots
into the scoring functions.
