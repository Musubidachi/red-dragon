# red-dragon — Architecture (MD-Layer Map)

This document maps the eight-layer architecture from
`trading_framework_evolution_and_market_structure_project.md` onto the actual
Java modules and packages in this repository.

The repo is a multi-module Maven project. Only `app` is bootable; the
`lib-*` modules are plain jars consumed by `app`.

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
| **L1 — Opportunity Discovery** | Surface possible opportunities cheaply (SEC filings, manual entry, future RSS/scanners) | `lib-ingestion/sec/`, `lib-ingestion/service/` |
| **L2 — Data Ingestion** | Normalize raw feeds into timestamped, source-tracked records | `lib-ingestion/model/`, `lib-marketdata/provider/`, `lib-marketdata/service/` |
| **L3 — Structural Validation** | Is the catalyst real, material, and meaningful? | `lib-analytics/structural/` (scorers) + `lib-validation/engine/` (hard gates) |
| **L4 — Market-State Classification** | What regime are we in? Is it supportive? | `lib-analytics/classification/` |
| **L5 — Deployment Engine** | How aggressive should the tier be? | `lib-analytics/deployment/` (confidence input) + `lib-validation/engine/DeploymentResolver` (tier decision) |
| **L6 — Narrative Propagation** | Is the story expanding or saturated? | `lib-analytics/propagation/` |
| **L7 — Exit / Equilibrium Compression** | Has restoration completed or asymmetry compressed? | (planned) — currently expressed via `EquilibriumPhaseAnalyzer` in `lib-analytics/classification/` |
| **L8 — Meta-System Adaptation** | Is our own edge drifting? | `lib-analytics/meta/` |

## Module responsibilities

| Module | What it owns | Layers |
|---|---|---|
| `app` | Spring Boot main, HTTP controllers, bean wiring, the only bootable jar | (wires all layers) |
| `lib-ingestion` | External-source candidate ingestion (SEC EDGAR, manual entry) | L1, L2 (candidates) |
| `lib-marketdata` | OHLCV retrieval, feature derivation (VWAP, ATR, range position, liquidity), provider adapters | L2 (market data), feeds L4 |
| `lib-analytics` | Pure-function scorers and the analytics orchestrator | L3, L4, L5 (input), L6, L8 |
| `lib-validation` | Hard gates, score aggregation, verdict + tier decision | L3 (gates), L5 (tier) |
| `lib-persistence` | JPA entities, repositories, Flyway migrations | cross-cutting |
| `lib-backtest` | Deterministic replay harness | supports L8 |
| `lib-execution` | (Doc-only) future Schwab order placement, dry-run gated | (planned) post-L5 |

## Where each MD-layer file lives

### L1 / L2 — Discovery + Ingestion
* `lib-ingestion/src/main/java/dev/reddragon/ingestion/sec/`
  — SEC EDGAR client, 8-K mapping, candidate building
* `lib-ingestion/src/main/java/dev/reddragon/ingestion/service/`
  — Manual entry pipeline
* `lib-ingestion/src/main/java/dev/reddragon/ingestion/model/TradeCandidate.java`
  — The normalized record every L1/L2 source emits

### L3 — Structural Validation
* `lib-analytics/.../structural/AdversarialValidationAnalyzer.java` — finds contradictions
* `lib-analytics/.../structural/AsymmetryScorer.java` — risk/reward asymmetry
* `lib-analytics/.../structural/DilutionRiskScorer.java` — dilution risk
* `lib-analytics/.../structural/MaterialityImpactScorer.java` — catalyst materiality
* `lib-validation/.../engine/HardGateEvaluator.java` — fail-fast hard rules

### L4 — Market-State Classification
* `lib-analytics/.../classification/RegimeCompatibilityScorer.java` — top-level regime label
* `lib-analytics/.../classification/EquilibriumQualityScorer.java` — mean-reversion quality
* `lib-analytics/.../classification/EquilibriumPhaseAnalyzer.java` — intraday phase
* `lib-analytics/.../classification/DirectionalPersistenceScorer.java` — trend persistence
* `lib-analytics/.../classification/VolatilityExpansionScorer.java` — realized-vol expansion
* `lib-analytics/.../classification/VwapInteractionScorer.java` — VWAP behavior
* `lib-analytics/.../classification/LiquidityTextureScorer.java` — depth, spread, participation
* `lib-analytics/.../classification/OptionsFlowScorer.java` — unusual options activity
* `lib-analytics/.../service/MarketStateClassifier.java` — standalone classifier endpoint

### L5 — Deployment
* `lib-analytics/.../deployment/DeploymentConfidenceScorer.java` — confidence input
* `lib-validation/.../engine/DeploymentResolver.java` — final tier decision

### L6 — Narrative Propagation
* `lib-analytics/.../propagation/NarrativeExpansionScorer.java`
* `lib-analytics/.../propagation/PropagationPhaseAnalyzer.java`
* `lib-analytics/.../propagation/ReflexivityScorer.java`
* `lib-analytics/.../propagation/SectorPropagationScorer.java`

### L8 — Meta-System Adaptation
* `lib-analytics/.../meta/LiveContextAdaptationAnalyzer.java`
* `lib-analytics/.../meta/LongHorizonCalibrationAnalyzer.java`

## Junior-developer reading order

1. `README.md` — what the system does and the pipeline shape
2. This file (`ARCHITECTURE.md`) — how MD layers map to modules
3. `lib-ingestion/src/main/java/dev/reddragon/ingestion/model/TradeCandidate.java`
   — the value object that flows through every layer
4. `lib-analytics/src/main/java/dev/reddragon/analytics/service/DeterministicAnalyticsService.java`
   — the orchestrator that calls everything in turn
5. `lib-validation/src/main/java/dev/reddragon/validation/engine/DisequilibriumValidationEngine.java`
   — the final verdict pipeline
6. `app/src/main/java/dev/reddragon/app/pipeline/CandidatePipelineOrchestrator.java`
   — wires ingestion → enrichment → analytics → validation → persistence

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
  leak past `lib-marketdata/.../provider/schwab/`.
* **`@Value` constructors do validation.** If a field has a normalized range,
  the constructor must enforce it — there is no second line of defense.
