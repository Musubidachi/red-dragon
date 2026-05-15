# red-dragon — Architecture (MD-Layer Map)

This document maps the eight-layer architecture from
`trading_framework_evolution_and_market_structure_project.md` onto the actual
Java modules and packages in this repository.

The repo is a multi-module Maven project. Only `app` is bootable; the
`lib-*` modules are plain jars consumed by `app`.

## Project conventions

Every module is laid out the same way:

```
<module>/src/main/java/dev/reddragon/<module>/
    models/      — value objects, DTOs, snapshots, records, enums
    domains/     — JPA entities (lib-persistence only)
    services/    — business logic; sub-packaged by feature/MD-layer where useful
    controllers/ — HTTP controllers (app only)
    utilities/   — pure static helpers
    config/      — Spring configuration, properties classes
```

The MD-layer mapping is preserved one level inside `services/` (e.g.
`lib-analytics/services/structural/`, `services/classification/`, etc.) so the
two axes (type and layer) are both visible in the file tree.

## At a glance

```
candidate in  →  enrich  →  classify  →  validate  →  verdict out
   (L1/L2)      (L2)        (L4/L6)     (L3/L5)
                                                ↑
                                          (L8 meta watches)
```

## Layer-by-layer mapping

| MD layer | Purpose | Code home |
|---|---|---|
| **L1 — Opportunity Discovery** | Surface possible opportunities cheaply (SEC filings, manual entry, future RSS/scanners) | `lib-ingestion/services/sec/`, `lib-ingestion/services/` |
| **L2 — Data Ingestion** | Normalize raw feeds into timestamped, source-tracked records | `lib-ingestion/models/`, `lib-marketdata/services/provider/`, `lib-marketdata/services/` |
| **L3 — Structural Validation** | Is the catalyst real, material, and meaningful? | `lib-analytics/services/structural/` (scorers) + `lib-validation/services/engine/` (hard gates) |
| **L4 — Market-State Classification** | What regime are we in? Is it supportive? | `lib-analytics/services/classification/` |
| **L5 — Deployment Engine** | How aggressive should the tier be? | `lib-analytics/services/deployment/` (confidence input) + `lib-validation/services/engine/DeploymentResolver` (tier decision) |
| **L6 — Narrative Propagation** | Is the story expanding or saturated? | `lib-analytics/services/propagation/` |
| **L7 — Exit / Equilibrium Compression** | Has restoration completed or asymmetry compressed? | `lib-analytics/services/exit/` (scorer) + `lib-analytics/models/exit/` (DTOs) — `EquilibriumCompressionScorer` returns `ExitSignal` (HOLD / TIGHTEN / SCALE_OUT / EXIT_NOW) via `POST /api/exit-signal` |
| **L8 — Meta-System Adaptation** | Is our own edge drifting? | `lib-analytics/services/meta/` + `app/services/pipeline/CalibrationOutcomeService.java` |

## Module responsibilities

| Module | What it owns | Layers |
|---|---|---|
| `app` | Spring Boot main, HTTP controllers, bean wiring, the only bootable jar | (wires all layers) |
| `lib-ingestion` | External-source candidate ingestion (SEC EDGAR, manual entry) | L1, L2 (candidates) |
| `lib-marketdata` | OHLCV retrieval, feature derivation (VWAP, ATR, range position, liquidity), provider adapters | L2 (market data), feeds L4 |
| `lib-analytics` | Pure-function scorers and the analytics orchestrator | L3, L4, L5 (input), L6, L7, L8 |
| `lib-validation` | Hard gates, score aggregation, verdict + tier decision | L3 (gates), L5 (tier) |
| `lib-persistence` | JPA domain entities, repositories, mappers, Flyway migrations | cross-cutting |
| `lib-backtest` | Deterministic replay harness | supports L8 |
| `lib-execution` | (Doc-only) future Schwab order placement, dry-run gated | (planned) post-L5 |

## Where each MD-layer file lives

### L1 / L2 — Discovery + Ingestion
* `lib-ingestion/services/sec/` — SEC EDGAR client, 8-K mapping, candidate building
* `lib-ingestion/services/ManualCandidateIngestionService.java` — manual entry pipeline
* `lib-ingestion/models/TradeCandidate.java` — the normalized record every L1/L2 source emits
* `lib-ingestion/models/sec/SecFiling.java` — denormalized SEC filing
* `lib-ingestion/config/SecApiProperties.java` — SEC EDGAR connection config

### L3 — Structural Validation
* `lib-analytics/services/structural/AdversarialValidationAnalyzer.java` — finds contradictions
* `lib-analytics/services/structural/AsymmetryScorer.java` — risk/reward asymmetry
* `lib-analytics/services/structural/DilutionRiskScorer.java` — dilution risk
* `lib-analytics/services/structural/MaterialityImpactScorer.java` — catalyst materiality
* `lib-validation/services/engine/HardGateEvaluator.java` — fail-fast hard rules

### L4 — Market-State Classification
* `lib-analytics/services/classification/RegimeCompatibilityScorer.java` — top-level regime label
* `lib-analytics/services/classification/EquilibriumQualityScorer.java` — mean-reversion quality
* `lib-analytics/services/classification/EquilibriumPhaseAnalyzer.java` — intraday phase
* `lib-analytics/services/classification/DirectionalPersistenceScorer.java` — trend persistence
* `lib-analytics/services/classification/VolatilityExpansionScorer.java` — realized-vol expansion
* `lib-analytics/services/classification/VwapInteractionScorer.java` — VWAP behavior
* `lib-analytics/services/classification/LiquidityTextureScorer.java` — depth, spread, participation
* `lib-analytics/services/classification/OptionsFlowScorer.java` — unusual options activity
* `lib-analytics/services/MarketStateClassifier.java` — standalone classifier endpoint

### L5 — Deployment
* `lib-analytics/services/deployment/DeploymentConfidenceScorer.java` — confidence input
* `lib-validation/services/engine/DeploymentResolver.java` — final tier decision

### L6 — Narrative Propagation
* `lib-analytics/services/propagation/NarrativeExpansionScorer.java`
* `lib-analytics/services/propagation/PropagationPhaseAnalyzer.java`
* `lib-analytics/services/propagation/ReflexivityScorer.java`
* `lib-analytics/services/propagation/SectorPropagationScorer.java`

### L7 — Exit / Equilibrium Compression
* `lib-analytics/services/exit/EquilibriumCompressionScorer.java` — composes asymmetry compression, propagation phase, equilibrium phase, and range extension into a single recommendation
* `lib-analytics/models/exit/ExitSignal.java` — output: recommendation + compression score + notes
* `lib-analytics/models/exit/ExitSignalInput.java` — input value object
* `lib-analytics/models/exit/ExitRecommendation.java` — HOLD / TIGHTEN / SCALE_OUT / EXIT_NOW

### L8 — Meta-System Adaptation
* `lib-analytics/services/meta/LiveContextAdaptationAnalyzer.java`
* `lib-analytics/services/meta/LongHorizonCalibrationAnalyzer.java`
* `app/services/pipeline/CalibrationOutcomeService.java` — persistence + rolling summary; closes the L8 loop without execution being live
* `lib-persistence/domains/CalibrationOutcomeEntity.java` + Flyway `V2__calibration_outcome.sql`

## Junior-developer reading order

1. `README.md` — what the system does and the pipeline shape
2. This file (`ARCHITECTURE.md`) — how MD layers map to modules
3. `lib-ingestion/src/main/java/dev/reddragon/ingestion/models/TradeCandidate.java` — the value object that flows through every layer
4. `lib-analytics/src/main/java/dev/reddragon/analytics/services/DeterministicAnalyticsService.java` — the orchestrator that calls everything in turn
5. `lib-validation/src/main/java/dev/reddragon/validation/services/engine/DisequilibriumValidationEngine.java` — the final verdict pipeline
6. `app/src/main/java/dev/reddragon/app/services/pipeline/CandidatePipelineOrchestrator.java` — wires ingestion → enrichment → analytics → validation → persistence

Every public package has a `package-info.java` calling out its MD layer.
When in doubt, open that file first.

## Project conventions

* **Lombok is the default** for value objects (`@Value` + `@Accessors(fluent = true)`),
  service constructors (`@RequiredArgsConstructor`), and loggers (`@Slf4j`).
  Do not write manual builders, getters, or `LoggerFactory.getLogger(...)`.
* **No nested classes.** Every class, enum, record, and interface lives in
  its own top-level file. Cache entries and Jackson DTOs included.
* **Pure functions in `lib-analytics`.** No I/O, no static state, no portfolio.
  Each scorer takes a snapshot in and returns a 0.0–1.0 score out.
* **Provider adapters stay sealed.** Schwab DTOs are package-private and never
  leak past `lib-marketdata/services/provider/schwab/`.
* **`@Value` constructors do validation.** If a field has a normalized range,
  the constructor must enforce it — there is no second line of defense.
