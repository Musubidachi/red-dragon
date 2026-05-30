# red-dragon

Probabilistic market-state intelligence platform.

## What This Is

A trade-candidate pipeline. Ideas flow in from external sources such as SEC
EDGAR filings or manual entry, get enriched with market-data context, scored
against market regime and asymmetry signals, then filtered by a validation
layer. What survives is presented to the trader with the reasoning chain
attached.

The application workflow is candidate-in / verdict-out. It does not place live
broker orders or track a live portfolio. It stores candidate history,
validation results, trader notes, calibration outcomes, imported trade-history
samples, market observations, and Schwab OAuth state so decisions can be
reviewed and calibrated later. It augments discretionary decisions; it does not
replace them.

Only `app` produces a bootable Spring Boot artifact. All Maven reactor modules
under `lib-*` are plain jars consumed by `app` or by future wiring.
`lib-execution` is a plain jar for provider-neutral dry-run broker execution;
it does not place live broker orders.
`RED_DRAGON_EXECUTION_MODE` defaults to `DRY_RUN`. `LIVE` is only accepted if
`RED_DRAGON_EXECUTION_LIVE_ENABLED=true`, and the current live implementation
still fails closed with a placeholder client.

By default the app binds to `127.0.0.1` via `SERVER_ADDRESS`. Set
`SERVER_ADDRESS=0.0.0.0` when you need Docker host networking, remote-machine
access, or an external load balancer to reach the service.

All `/api/**` endpoints require `Authorization: Bearer <token>`. Configure the
token with `RED_DRAGON_API_KEY`; local development defaults to
`dev-red-dragon-api-key`. CORS pre-flight requests remain unauthenticated.
The dashboard's candidate feed uses browser `EventSource`, so only
`/api/review/candidates/stream` also accepts the same key as an `apiKey` query
parameter. Tune its cadence with `RED_DRAGON_REVIEW_STREAM_REFRESH_MS` and
`RED_DRAGON_REVIEW_STREAM_TIMEOUT_MS`.

Scheduled intraday market-state snapshots are disabled by default. Enable them
with `RED_DRAGON_MARKET_STATE_SCHEDULER_ENABLED=true` and provide
`RED_DRAGON_MARKET_STATE_SCHEDULER_SYMBOLS` as a comma-separated ticker list.
Cadence, zone, lookback, and bar interval are controlled by
`RED_DRAGON_MARKET_STATE_SCHEDULER_CRON`,
`RED_DRAGON_MARKET_STATE_SCHEDULER_ZONE`,
`RED_DRAGON_MARKET_STATE_SCHEDULER_LOOKBACK_HOURS`, and
`RED_DRAGON_MARKET_STATE_SCHEDULER_INTERVAL_MINUTES`.

## Pipeline

```text
[ ingestion ]  SEC filings, manual candidates
     |
     v
[ enrichment ] market-data features (VWAP, ATR, range position, liquidity, gap)
     |
     v
[ analytics ]  regime classifier + asymmetry scorer
     |
     v
[ validation ] hard gates + score aggregation -> PASS / WATCH / REJECT + deployment tier
     |
     v
[ review ]     trader sees survivors with full reasoning chain and risk flags
```

## Module Status

| Module | Current role | Implemented today | Still needed | Details |
| --- | --- | --- | --- | --- |
| `app` | Wires the platform and exposes HTTP endpoints. | Spring Boot main, controllers, pipeline orchestration, scheduler/status surfaces, calibration/history/backtest/review endpoints, Schwab OAuth controllers. It is the only bootable artifact. | Keep controller docs verified against source before expanding this README; continue to keep business logic in libraries. | [ARCHITECTURE.md](ARCHITECTURE.md) |
| `lib-math` | Shared numeric helper jar. | Clamping, normalized-score validation, non-negative guards, weighted averages, `safePercentChange`, `floorAtZero`, and tests for NaN/infinity handling. | No open module-local issues are currently tracked; keep helper behavior pinned as new callers appear. | [lib-math/REVIEW.md](lib-math/REVIEW.md) |
| `lib-domain` | Shared cross-module value objects and enums. | Candidates, market bars, market snapshots, analytics snapshots, validation inputs/results, verdicts, risk flags, calibration reports, exit DTOs, closed-set liquidity/score enums, centralized score policy, builder support for `CandidateValidationInput`, and focused tests for high-risk value-object validation. | No open module-local issues are currently tracked; add narrow tests as new shared models gain callers. | [lib-domain/REVIEW.md](lib-domain/REVIEW.md) |
| `lib-ingestion` | L1 opportunity discovery and candidate-side L2 ingestion. | Manual candidate ingestion; SEC submissions client by CIK or ticker; ticker-to-CIK and CIK-to-tickers lookup with TTL/fail-stale refresh; SEC HTTP timeouts/retry/rate limiting; 8-K taxonomy; form variant handling; primary-document body fetches; Form 4 ownership XML parsing; opt-in Form 4 body-derived `TradeCandidate` creation; deterministic manual IDs. | No open module-local issues are currently tracked; deferred 13D/G, offering, XBRL, RSS firehose, and LLM scheduler work remains design-forward. | [lib-ingestion/README.md](lib-ingestion/README.md), [lib-ingestion/REVIEW.md](lib-ingestion/REVIEW.md) |
| `lib-marketdata` | Market-data provider adapters and deterministic feature derivation. | Schwab, Yahoo, Noop, and composite providers; daily/intraday bars and quotes; status-aware Schwab retry/backoff; ATR via shared OHLC contract; Wilder ATR; session-bound VWAP; realized volatility; feature calculators; replay/stream helpers; analytics-owned market score enrichment; historical calibration fixtures through `lib-backtest`. | No open module-local issues are currently tracked; add further numeric branch tests only as new edge cases appear. | [lib-marketdata/README.md](lib-marketdata/README.md), [lib-marketdata/REVIEW.md](lib-marketdata/REVIEW.md) |
| `lib-analytics` | Pure deterministic scorers and analytics orchestration. | Layered L3-L8 scorers, `DeterministicAnalyticsService`, standalone market-state classifier, exit-signal scorer, configurable analytics-owned calibration drift thresholds, and market-data snapshot scoring. The orchestrator delegates to scorer classes, and note-emitting scorers return immutable result objects. | No open module-local issues are currently tracked; keep analytics side-effect free and threshold changes explicit. | [lib-analytics/README.md](lib-analytics/README.md), [lib-analytics/REVIEW.md](lib-analytics/REVIEW.md) |
| `lib-validation` | Hard gates, aggregate verdicts, and deployment tiers. | Validation service facade, gate evaluator, factor factory, confidence scorer, verdict resolver, deployment resolver, risk flags, summary formatter, bindable threshold properties, named threshold profiles, and OBSERVE-threshold support. | No open module-local issues are currently tracked; keep profile invariants and binding tests updated as thresholds evolve. | [lib-validation/README.md](lib-validation/README.md), [lib-validation/REVIEW.md](lib-validation/REVIEW.md) |
| `lib-persistence` | JPA entities, repositories, mapper, and Flyway schema. | Runtime migrations through V18, 17 tables including normalized validation reasons, calibration report history, and market-state snapshot history, optimistic locking on mutable tables, audit timestamps, backtest FK, validation-verdict idempotency key, `TEXT` freeform columns, trade-history storage, id-free generated-id builders, and AES-GCM Schwab token conversion for new writes. | No open module-local issues are currently tracked; re-save or purge historical plaintext Schwab token rows if retroactive cleanup is required. | [lib-persistence/README.md](lib-persistence/README.md), [lib-persistence/REVIEW.md](lib-persistence/REVIEW.md) |
| `lib-backtest` | Deterministic replay harness supporting L8 calibration. | `BacktestFrame`, `BacktestOutcome`, `BacktestMetrics`, `BacktestReport`, and stateless `BacktestReplayEngine` running marketdata -> analytics -> validation through the shared `CandidateValidationInputFactory`; tests cover determinism, metrics, direct-engine equivalence, historical calibration fixtures, and app wiring. | No open module-local issues are currently tracked; keep fixtures updated when upstream scoring contracts intentionally change. | [lib-backtest/README.md](lib-backtest/README.md), [lib-backtest/REVIEW.md](lib-backtest/REVIEW.md) |
| `lib-execution` | Provider-neutral broker execution contracts and dry-run lifecycle modeling. | Maven reactor module with `BrokerClient`, account/position/order models, equity and single-leg option order requests, dry-run order placement/cancellation/fill recording/querying, idempotent client order IDs, and a fail-closed live placeholder. | Live Schwab account/order HTTP integration, order-lifecycle audit state, app confirmation gates, and real fill reconciliation remain deferred. Schwab OAuth traffic now records broker-call audit rows in persistence. | [lib-execution/SCHWAB_EXECUTION.md](lib-execution/SCHWAB_EXECUTION.md) |

See [REVIEW.md](REVIEW.md) for the current repo-wide work-remains summary,
[ISSUES.md](ISSUES.md) for current issue debt, [ENHANCEMENTS.md](ENHANCEMENTS.md)
for the remaining product/UI backlog, and
[MARKDOWN_CLEANUP_GUIDE.md](MARKDOWN_CLEANUP_GUIDE.md) for future doc cleanup
rules.

## Package Layout

Most modules follow this shape:

```text
<module>/src/main/java/dev/reddragon/<module>/
    models/      value objects, DTOs, snapshots, records, enums
    domains/     JPA entities (lib-persistence only)
    services/    business logic
    controllers/ HTTP controllers (app only)
    utilities/   pure static helpers
    config/      Spring configuration and properties classes
```

`lib-domain` owns shared domain language under `dev.reddragon.domain.models`.
`lib-math` owns shared numeric helpers under `dev.reddragon.math`. Module-owned
wire DTOs, controller DTOs, provider DTOs, and JPA entities intentionally stay
in their owning modules.

## Project Conventions

* Lombok is the default for value objects, service constructors, and loggers.
* No nested classes. Every class, enum, record, and interface lives in its own
  top-level file.
* Shared domain models live in `lib-domain`; shared numeric helpers live in
  `lib-math`.
* `lib-analytics` is deterministic and side-effect free.
* Schwab provider DTOs stay inside `lib-marketdata/services/provider/schwab/`.

## HTTP API Surface

This table is a working index of the current API. Before adding endpoints here,
verify paths against controllers in `app`; controller source is the runtime
truth.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/` | Static review dashboard. |
| GET | `/health` | Liveness check. |
| GET | `/actuator/metrics` | Micrometer metrics. |
| POST | `/api/pipeline/manual` | Run a manually supplied candidate through the full pipeline. |
| GET | `/api/pipeline/sec/{cik}` | Fetch SEC filings for a CIK and run them through the pipeline. |
| GET | `/api/pipeline/sec/ticker/{ticker}` | Resolve ticker to CIK, fetch SEC filings, and run them through the pipeline. |
| GET | `/api/pipeline/sec/watch-list` | Run configured CIKs through the SEC pipeline. |
| GET | `/api/pipeline/status` | Pipeline and scheduler status. |
| GET | `/api/analysis/{ticker}` | Analyze one ticker using market data, SEC lookup, deterministic rules, optional LLM web research, and source-coverage confidence. |
| POST | `/api/review/manual` | Ad-hoc manual review without persistence. |
| GET | `/api/review/candidates` | Recent PASS / WATCH verdicts with reasoning. |
| GET | `/api/review/candidates/stream` | Server-Sent Events stream that emits refreshed candidate lists. Browser `EventSource` callers pass the local API key as `apiKey`; normal API clients should keep using bearer auth. |
| GET | `/api/candidates` | Stored candidates. |
| GET | `/api/candidates/{symbol}/history` | Candidate history for one symbol. |
| GET | `/api/candidates/id/{candidateId}` | Candidate lookup by id. |
| POST | `/api/candidates/{id}/notes` | Add a trader note to a candidate. |
| GET | `/api/candidates/{id}/notes` | List trader notes for a candidate. |
| GET | `/api/stats` | General pipeline stats. |
| GET | `/api/stats/opportunity-quality` | Conviction mix, deployment-tier mix, and top symbols. |
| POST | `/api/backtest` | Run a replay against historical frames. |
| GET | `/api/backtest/results/{runId}` | Stored backtest results for a run. |
| GET | `/api/backtest/runs` | Stored backtest run ids. |
| GET | `/api/market-data/{symbol}/daily` | Historical daily bars from the configured provider chain. |
| GET | `/api/market-data/{symbol}/daily/stored` | Stored daily bars for a symbol/date range. |
| GET | `/api/market-data/{symbol}/intraday` | Intraday bars from the configured provider chain. |
| GET | `/api/market-data/{symbol}/intraday/stored` | Stored intraday bars for a symbol/time range. |
| GET | `/api/market-data/{symbol}/quote` | Latest quote from the configured provider chain. |
| GET | `/api/market-data/{symbol}/quote/stored` | Stored quote observations for a symbol. |
| POST | `/api/market-state/classify` | Classify a supplied market-state snapshot. |
| GET | `/api/market-state/snapshots` | Recent persisted intraday market-state snapshots, optionally filtered by `symbol`. |
| POST | `/api/market-structure/intraday` | Derive an intraday structure snapshot from bars. |
| POST | `/api/exit-signal` | L7 exit signal: HOLD / TIGHTEN / SCALE_OUT / EXIT_NOW. |
| POST | `/api/calibration` | Append realized outcomes and return a drift report. |
| GET | `/api/calibration` | Current calibration drift report. |
| GET | `/api/calibration/reports` | Recent persisted calibration drift reports. |
| GET | `/api/calibration/summary` | Rolling summary: win rate, average return, average drawdown. |
| GET | `/api/calibration/summary/batch` | Per-symbol rolling summaries for a comma-separated symbol list. |
| GET | `/api/calibration/outcomes` | Recent outcome rows. |
| GET | `/api/calibration/outcomes/export` | CSV export of recent outcomes. |
| GET | `/api/calibration/outcomes/{symbol}` | Recent outcomes for one symbol. |
| DELETE | `/api/calibration/outcomes` | Delete all calibration outcomes. |
| DELETE | `/api/calibration/outcomes/{symbol}` | Delete calibration outcomes for one symbol. |
| POST | `/api/history/import` | Import and normalize trade-history CSV text. |
| GET | `/api/history/imports` | Recent trade-history import batches. |
| GET | `/api/history/imports/{batchId}` | One import batch with normalized records. |
| GET | `/api/history/trades` | Stored normalized trade-history records, optionally filtered by ticker. |
| GET | `/api/schwab/oauth/authorize-url` | URL the trader visits to grant Schwab API access. |
| GET | `/api/schwab/oauth/callback` | Exchange Schwab OAuth code and persist tokens. |
| POST | `/api/schwab/oauth/refresh` | Manually refresh Schwab access token. |
| GET | `/api/regime/history` | Stored regime / analytics history. |
| GET | `/api/verdicts/{candidateId}/summary` | Display-ready validation summary. |
| POST | `/api/verdicts/{id}/override` | Record a manual verdict override. |
| GET | `/api/verdicts/stats` | Verdict distribution stats. |
| GET | `/api/validation/profiles` | Available validation threshold profiles. |
| GET | `/v3/api-docs` | Generated OpenAPI JSON. |
| GET | `/swagger-ui/index.html` | Swagger UI for local API inspection. |

Calibration also exposes consolidated detail summaries at
`/api/calibration/summary/details` and
`/api/calibration/summary/{symbol}/details`. Older scalar convenience endpoints
under `/api/calibration/summary/**` remain available for compatibility, but now
return deprecation headers pointing callers to the detail summaries.
