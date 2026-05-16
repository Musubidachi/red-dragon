# lib-analytics

Pure deterministic scoring for the disequilibrium pipeline. Analytics does not
fetch data, persist data, or know about account state. It consumes candidate and
market snapshots and emits score-bearing snapshots for validation and review.

## MD-Layer Mapping

| Package | MD layer | Job |
| --- | --- | --- |
| `services/structural/` | L3 | Catalyst reality, materiality, dilution risk, asymmetry, adversarial checks. |
| `services/classification/` | L4 | Regime compatibility, equilibrium quality, VWAP behavior, liquidity texture, volatility, options flow, trend persistence. |
| `services/deployment/` | L5 | Deployment confidence input. Final tiering lives in `lib-validation`. |
| `services/propagation/` | L6 | Narrative expansion, propagation phase, reflexivity, sector propagation. |
| `services/exit/` | L7 | Equilibrium compression and exit/tightening signal generation. |
| `services/meta/` | L8 | Live context adaptation and long-horizon calibration drift. |
| `services/` | cross-layer | Orchestrators such as `DeterministicAnalyticsService` and `MarketStateClassifier`. |
| `models/` | shared | Snapshots, labels, score breakdowns, calibration reports, and exit DTOs. |
| `utilities/` | shared | Pure math helpers. |

## Public API

Other modules primarily import:

* `DeterministicAnalyticsService` - main pipeline entry. Takes a
  `TradeCandidate` and `MarketDataSnapshot`, returns an `AnalyticsSnapshot`.
* `MarketStateClassifier` - standalone regime classifier exposed through
  `POST /api/market-state/classify`.
* `EquilibriumCompressionScorer` - L7 exit signal service exposed through
  `POST /api/exit-signal`.
* `LongHorizonCalibrationAnalyzer` - L8 drift analysis from realized outcomes.

The per-layer scorers are implementation details even where they are public for
testing and package-boundary simplicity.

## Rules For This Module

1. Pure functions only: no I/O, no persistence, no static mutable state, no
   portfolio awareness.
2. Same input means same output.
3. Scores carry explanation data so review is auditable.
4. Keep helper classes top-level; no nested classes.
5. Put new scorers in the layer package matching their job.

## Testing Expectations

Tests should cover deterministic scoring, threshold boundaries, missing or weak
snapshots, reason-code generation, and aggregation math. Do not depend on live
market data in analytics tests.
