# red-dragon — Roadmap

Last updated: 2026-05-27

This roadmap sequences work from the current state of the platform to a fully
production-ready personal trading assistant. It is organized into four phases.
Each phase delivers a usable, stable state on its own — later phases build on
earlier ones but do not invalidate them.

Reference documents:
- [README.md](README.md) — platform overview and current module status
- [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) — detailed gap analysis
- [ISSUES.md](ISSUES.md) — specific code-level issues (ISS-NNN)
- [ENHANCEMENTS.md](ENHANCEMENTS.md) — UI and backend improvements (ENH-NNN)

---

## Phase 0 — Safe to run daily (Blockers only)

**Goal:** The platform can be left running unattended without risk of data loss,
unauthenticated access, or silently inconsistent state. This is the minimum bar
before using the system on any network beyond `localhost`.

**Estimated scope:** Small — all items are contained code changes, no new modules.

---

### P0-1 — Persistent database

**Refs:** PRODUCTION_READINESS.md Gap 2.1

Replace the H2 in-memory default with a persistent PostgreSQL instance.

- Add `org.postgresql:postgresql` JDBC driver dependency to `app/pom.xml`
- Create a `docker-compose.yml` at the repo root with:
  - A `postgres:16` container with a named volume for data persistence
  - The red-dragon app container with all required env vars listed
  - Port `8080` exposed
- Set `RED_DRAGON_SAMPLE_DATA_ENABLED=false` in the production env
- Document `RED_DRAGON_DB_URL`, `RED_DRAGON_DB_USERNAME`,
  `RED_DRAGON_DB_PASSWORD`, `RED_DRAGON_DB_DRIVER` in `README.md`
- Verify all 16 Flyway migrations run cleanly against Postgres

**Done when:** `docker-compose up` produces a running app backed by a durable
database that survives a container restart.

---

### P0-2 — API authentication

**Refs:** PRODUCTION_READINESS.md Gap 1.1, ISSUES.md ISS-002

Add a minimal authentication layer so the API is not world-writable.

- Add `spring-boot-starter-security` dependency
- Implement a `OncePerRequestFilter` that reads `Authorization: Bearer <token>`
  and validates it against a `RED_DRAGON_API_KEY` environment variable
- Permit without auth: `GET /health`, `GET /actuator/health`,
  `GET /api/schwab/oauth/callback` (Schwab redirects here externally)
- Return `401 Unauthorized` with the standard error envelope for missing or
  invalid tokens
- Document `RED_DRAGON_API_KEY` in `README.md`

**Done when:** Every non-exempt endpoint returns 401 without a valid API key,
and the daily review UI continues to work when the key is supplied.

---

### P0-3 — Wire CORS to environment variable

**Refs:** PRODUCTION_READINESS.md Gap 1.2, ISSUES.md ISS-001

Fix `WebConfiguration` to read the actual property instead of hardcoding `"*"`.

- Inject `@Value("${red-dragon.cors.allowed-origin-patterns}")` into
  `WebConfiguration`
- Split the comma-delimited string and pass the resulting array to
  `allowedOriginPatterns(...)`
- Add `RED_DRAGON_CORS_ALLOWED_ORIGINS` to the env var documentation

**Done when:** Setting `RED_DRAGON_CORS_ALLOWED_ORIGINS=http://localhost:8080`
restricts cross-origin access to that origin only.

---

### P0-4 — Fix transactional boundary in BacktestController

**Refs:** PRODUCTION_READINESS.md Gap 3.3, ISSUES.md ISS-003

- Add `@Transactional` to `BacktestController.runBacktest`
- Verify `CalibrationOutcomeService.appendBacktestOutcomes` participates in the
  outer transaction (confirm it uses `Propagation.REQUIRED`, not
  `REQUIRES_NEW`)

**Done when:** A failure in `appendBacktestOutcomes` rolls back the
`backtestResultRepository.saveAll` writes.

---

### P0-5 — Fix RegimeHistoryController in-memory query

**Refs:** PRODUCTION_READINESS.md Gap 3.4, ISSUES.md ISS-004

- Add `findByObservedAtAfterOrderByObservedAtDesc(Instant since, Pageable pageable)`
  to `AnalyticsSnapshotRepository`
- Replace the `findAll()` + in-memory stream path in `RegimeHistoryController`
  with a bounded repository call (default cap: 50 rows)

**Done when:** `GET /api/regime/history` runs a single bounded DB query
regardless of whether a `regime` filter is supplied.

---

### P0-6 — Disable sample data in production

**Refs:** PRODUCTION_READINESS.md Gap 8.2

- Ensure `H2SampleDataLoader` is guarded by both
  `@ConditionalOnProperty(name = "red-dragon.sample-data.enabled")` and a
  check that the datasource is H2 (or simply that the property is `true`)
- Document that `RED_DRAGON_SAMPLE_DATA_ENABLED` must be `false` in
  production in the env var reference

**Done when:** Restarting the app against a production Postgres database does
not insert synthetic candidates or verdicts.

---

**Phase 0 exit state:** The platform is safe to run on a home network with a
persistent database, locked behind an API key, with stable transactional
guarantees.

---

## Phase 1 — Reliable daily driver

**Goal:** The platform can be used every trading day with confidence. Errors
surface clearly, the UI shows the full picture, and the SEC scheduler can be
left running unattended.

---

### P1-1 — Bean Validation on request models

**Refs:** PRODUCTION_READINESS.md Gap 4.1, ISSUES.md ISS-010, ISS-012

- Annotate `ManualReviewRequest`, `PipelineReviewRequest`, and
  `CalibrationOutcomeSampleRequest` with `@NotBlank` and
  `@DecimalMin("0.0")`/`@DecimalMax("1.0")` on all score fields
- Add `@Valid` to all `@RequestBody` parameters in affected controllers
- Add `@ExceptionHandler(MethodArgumentNotValidException.class)` to
  `GlobalExceptionHandler` returning a structured 400 with field-level errors
- Add `@ExceptionHandler(ConstraintViolationException.class)` similarly

**Done when:** POSTing an empty body to `/api/pipeline/manual` returns a 400
with a clear field-error message rather than flowing through as all-zero scores.

---

### P1-2 — Controller integration tests

**Refs:** PRODUCTION_READINESS.md Gap 5.1, ISSUES.md ISS-019

Add `@WebMvcTest` or `@SpringBootTest` + `MockMvc` tests for each controller.
Priority order:

1. `CandidatePipelineOrchestrator` (core pipeline, duplicate-skip path)
2. `CalibrationController` (POST + summary GET)
3. `PipelineReviewController` (manual and SEC paths)
4. `ManualReviewController`
5. `TradeHistoryImportController` (CSV parsing edge cases)
6. `CandidateHistoryController`, `VerdictOverrideController`,
   `TraderNoteController`

Each test should cover: happy path, missing required field (400), not-found (404
where applicable).

**Done when:** `mvn test` in `app` passes a suite that covers all controller
endpoints at least at the happy-path level.

---

### P1-3 — Fix MarketStructureController path

**Refs:** ISSUES.md ISS-005

- Change `@RequestMapping("/market-structure")` to
  `@RequestMapping("/api/market-structure")`

**Done when:** `POST /api/market-structure/intraday` is reachable from a browser
without a CORS error.

---

### P1-4 — Remove deprecated fields from API serialization

**Refs:** ISSUES.md ISS-006

- Add `@JsonIgnore` to `ValidationVerdictEntity.reasonCodes` and
  `ValidationVerdictEntity.explanations`

**Done when:** `GET /api/review/candidates` no longer includes `reasonCodes` or
`explanations` as top-level fields in each verdict JSON object.

---

### P1-5 — SEC scheduler hardening

**Refs:** PRODUCTION_READINESS.md Gap 8.1, ISSUES.md ISS-016

- In `StartupValidator.run(...)`, add a check: if
  `red-dragon.scheduler.sec.enabled=true` and the CIK list is blank, emit a
  `log.warn(...)` clearly identifying the misconfiguration
- Verify `SecWatchListScheduler` logs the number of candidates produced per
  run (0 candidates should be distinguishable from a skipped/disabled run)

**Done when:** Starting the app with `SEC_SCHEDULER_ENABLED=true` and no CIKs
produces a visible warning in the startup log.

---

### P1-6 — Log rolling and Schwab token expiry warning

**Refs:** PRODUCTION_READINESS.md Gap 3.5

- Add a `logback-spring.xml` that writes to a rolling file log
  (`logs/red-dragon.log`, max 30 days or 500MB)
- In `SchwabTokenRefresher`, add a check after each successful token refresh:
  if the refresh token expires within 24 hours, emit a `log.warn(...)` with
  the expiry timestamp and a prompt to re-authenticate

**Done when:** Application logs are written to a file that persists across
restarts, and a Schwab token nearing expiry produces a visible warning.

---

### P1-7 — UI: REJECT tab and candidate detail drill-down

**Refs:** ENHANCEMENTS.md ENH-002, ENH-003

These are the two highest-value zero-backend-work UI additions:

- Add "Reject" pill to the filter bar; call `?verdicts=REJECT` using the same
  card renderer
- Make candidate cards clickable: fetch `GET /api/candidates/id/{candidateId}`
  and render a detail panel showing all market snapshot fields, all 5 analytics
  scores, the full reason list, trader notes, and any override

**Done when:** A trader can see why a candidate was rejected and drill into the
full analytical breakdown from the main review page.

---

### P1-8 — UI: pipeline status and verdict stats

**Refs:** ENHANCEMENTS.md ENH-001, ENH-012

- Replace the static header timestamp with a status row fetching
  `GET /api/pipeline/status` (candidateCount, verdictCount)
- Add verdict distribution stat cards from `GET /api/verdicts/stats` (PASS /
  WATCH / REJECT counts for the active lookback window)

**Done when:** The dashboard header shows live pipeline counts and the stats bar
shows today's verdict distribution alongside the calibration metrics.

---

### P1-9 — Document all environment variables

**Refs:** PRODUCTION_READINESS.md Gap 8.4

Add a `DEPLOYMENT.md` (or a "Deployment" section in `README.md`) listing every
`${...}` environment variable from `application.yml` with:
- Variable name
- Purpose
- Default value
- Whether it is required for production

**Done when:** An operator can configure the system from scratch using only this
document.

---

**Phase 1 exit state:** The platform is a reliable daily driver. The SEC
scheduler can run unattended. Errors surface as structured 400s rather than
silent wrong answers. The UI shows all three verdicts and full candidate detail.

---

## Phase 2 — Full-featured review surface

**Goal:** The UI exposes the complete analytical capability of the backend. A
trader can do everything from the browser that was previously only possible via
direct API calls: ticker analysis, backtesting, outcome journaling, trade history
import, and SEC watch-list management.

Items in this phase are all UI additions backed by already-implemented endpoints.
No new backend work is required.

---

### P2-1 — Ticker analysis search bar

**Refs:** ENHANCEMENTS.md ENH-010

Add a search input at the top of the page. On submit:
- Call `GET /api/analysis/{ticker}?lookbackDays=60&profile={activeProfile}`
- Render a full analysis card: analysis mode badge, source coverage
  (AVAILABLE / DEGRADED / UNAVAILABLE chips for market data / SEC / LLM),
  confidence score, LLM headline and summary, pipeline verdict and score,
  list of SEC candidates found, and any diagnostic notes

---

### P2-2 — Trader notes and verdict override

**Refs:** ENHANCEMENTS.md ENH-004, ENH-005

In the candidate detail panel (P1-7):
- Add a notes section: list existing notes (from `GET /api/candidates/{id}/notes`),
  text area + submit for new notes (`POST /api/candidates/{id}/notes`)
- Add an Override button: verdict selector + reason text → `POST /api/verdicts/{id}/override`
- Show an override badge on cards when `GET /api/verdicts/{candidateId}/summary`
  indicates one is present

---

### P2-3 — Opportunity quality and symbol performance

**Refs:** ENHANCEMENTS.md ENH-006, ENH-008, ENH-009

- Add an "Opportunity Quality" section from `GET /api/stats/opportunity-quality`:
  conviction bands (High / Medium / Low counts), top symbols by average score
- Fetch per-symbol calibration summary for each visible candidate card from
  `GET /api/calibration/summary/{symbol}` and show win rate inline
- Add a "Performance" section with top/worst outcomes from
  `GET /api/calibration/outcomes/top` and `/worst`

---

### P2-4 — Calibration outcomes journal

**Refs:** ENHANCEMENTS.md ENH-015

- Add a scrollable outcomes journal from `GET /api/calibration/outcomes/page`
  showing: symbol, date, return, drawdown, days held, thesis-worked
- Include a "Download CSV" link to `GET /api/calibration/outcomes/export`
- Add a submit form for manual outcome entry (`POST /api/calibration`)

---

### P2-5 — Trade history import panel

**Refs:** ENHANCEMENTS.md ENH-016

- Add an "Import" section with a CSV text area and submit button calling
  `POST /api/history/import`
- Show import result: `importedRows`, `totalRows`, any warnings
- Show recent trades from `GET /api/history/trades` in a filterable table

---

### P2-6 — Backtest runner and run browser

**Refs:** ENHANCEMENTS.md ENH-011

- Add a "Backtest" section with a form to name a run and supply frame JSON
- Submit triggers `POST /api/backtest`; show per-frame outcomes in a table
- Add a "Past Runs" list from `GET /api/backtest/runs` with clickable run IDs
  loading `GET /api/backtest/results/{runId}`

---

### P2-7 — SEC watch-list trigger and validation profile picker

**Refs:** ENHANCEMENTS.md ENH-017, ENH-018

- Add a "Run Watch List" panel: text input for CIKs, submit calls
  `GET /api/pipeline/sec/watch-list?ciks=...`; results stream into the candidate grid
- Add a profile selector dropdown (loaded from `GET /api/validation/profiles`)
  that persists the selected profile across all pipeline calls on the page

---

### P2-8 — Live quote and price chart on candidate detail

**Refs:** ENHANCEMENTS.md ENH-013, ENH-014

- Fetch `GET /api/market-data/{symbol}/quote` for each visible candidate symbol
  and show last price + day change percent on the card
- In the detail panel, render a 30-day close-price line chart from
  `GET /api/market-data/{symbol}/daily`

---

### P2-9 — Market state classifier tool and L8 health panel

**Refs:** ENHANCEMENTS.md ENH-019, ENH-020

- Add a "Classify Market" panel that accepts a symbol or intraday bar JSON,
  calls `POST /api/market-state/classify`, and renders the `MarketStateSignal`
  (regime label, sub-scores, supportive flag)
- Add a collapsible "Framework Health (L8)" section from `GET /api/calibration`
  showing the calibration drift report with color-coded status

---

### P2-10 — Backend cleanup

These backend-only items complete the platform quality improvements:

- Replace `Page<CandidateEntity>` in `CandidateController` with a
  `CandidateSummaryDto` (ENH-023 / ISS-011)
- Switch `ValidationVerdictEntity.reasons` to `FetchType.LAZY` (ENH-029 / ISS-018)
- Add `reddragon.pipeline.candidate.duplicate` Micrometer counter for duplicate
  skips (ISS-013)
- Consolidate the 20+ single-stat calibration endpoints (ENH-022 / ISS-009)
- Make `MarketStateController` and `MarketStructureController` inject their
  dependencies via Spring DI instead of `new` (ISS-008)

---

**Phase 2 exit state:** Every backend capability is reachable from the browser.
The platform is a complete, self-contained research and review tool.

---

## Phase 3 — Live execution (Schwab)

**Goal:** The platform can optionally place broker orders on confirmed PASS
verdicts. This phase is intentionally gated and sequenced to minimize financial
risk. Dry-run mode remains the default throughout; live trading requires an
explicit, additional opt-in at each step.

> **Prerequisite:** Schwab developer portal approval and a linked brokerage
> account must be obtained before any work in this phase begins. See
> [lib-execution/SCHWAB_EXECUTION.md](lib-execution/SCHWAB_EXECUTION.md) §3.

---

### P3-1 — Read-only Schwab account integration

Before any write operations are possible, implement and validate read-only
account state:

- Implement `SchwabHttpClient` (`RestClient` wrapper with bearer token injection,
  gzip, rate limiter, retry/backoff)
- Implement `SchwabBrokerClient.getAccountSummary()`,
  `.getPositions()`, `.getOrders(...)`, `.getOrder(...)`
- Add a `GET /api/execution/account` endpoint (returns account summary +
  positions in dry-run or live mode)
- Validate with mock HTTP tests; do not allow any live Schwab call before tests pass

---

### P3-2 — Broker audit log and execution mode config

Before any write operations:

- Add `broker_call_log` Flyway migration (timestamp, endpoint, request body
  with secrets redacted, response status + body, correlation id)
- Add `RED_DRAGON_EXECUTION_MODE=dry-run|live` environment variable
- Wire `executionMode` in app wiring: `DryRunBrokerClient` is always the default;
  `SchwabBrokerClient` is only active when `live` is explicitly set
- All `BrokerClient` calls must write an audit row regardless of mode

---

### P3-3 — Equity order placement (dry-run first)

- Implement `SchwabBrokerClient.placeOrder(EquityOrder)` calling
  `POST /trader/v1/accounts/{accountHash}/orders`
- Confirm idempotency key wiring: `clientOrderId` UUID generated at call site,
  stored in the audit log, used for reconciliation on network failure
- Test in dry-run mode end-to-end; do not merge until dry-run tests pass
- Add `cancelOrder(String orderId)` implementation

---

### P3-4 — Human confirmation gate

- Add a `POST /api/execution/orders/confirm` endpoint:
  - Accepts a pending order request (symbol, side, quantity, limitPrice)
  - Returns a `PendingOrderToken` (UUID, expiry 5 min)
- Add a `POST /api/execution/orders/place` endpoint that requires a valid
  `PendingOrderToken` before calling `placeOrder`
- No order can be placed without a preceding confirmation call

---

### P3-5 — Refresh token expiry workflow

- When `SchwabTokenRefresher` detects the refresh token expiring within 24h:
  - Emit a structured warning with the expiry timestamp
  - Surface a re-auth prompt in the UI (`GET /api/schwab/oauth/authorize-url`)
- When the refresh token has expired and a refresh attempt returns `400/401`:
  - Set all broker write operations to fail-closed (throw, do not retry)
  - Surface a clear "re-authentication required" error in the UI

---

### P3-6 — Single-leg option orders (covered calls)

The primary use case for the Roth IRA is covered-call premium:
`SELL_TO_OPEN` single-leg option orders on positions already held.

- Implement `SchwabBrokerClient.placeOrder(SingleLegOptionOrder)`
- Add OCC option symbol construction helper
- Add a "Sell Covered Call" flow to the UI: select held symbol → strike/expiry
  picker → limit price input → confirm → place

---

### P3-7 — Position-aware validation (optional)

Once live position reads are available, the validation layer can optionally
incorporate portfolio context:

- Add a `PortfolioContext` input to `CandidateValidationInput` (nullable;
  validation stays portfolio-blind if context is absent)
- Add a new `ReasonCode` for concentration risk: "position already at
  concentration limit" triggers REJECT regardless of score
- Wire `getPositions()` call into `CandidatePipelineOrchestrator` when
  `executionMode=live`

**Note:** Keep this optional and off by default. Validation must remain
meaningful without portfolio context.

---

**Phase 3 exit state:** The platform can confirm and place equity and covered-call
orders through a human-gated confirmation flow. Dry-run mode remains the default.
Every broker call is audited. The system fails closed on token expiry.

---

## Phase 4 — Longer-horizon improvements

These items improve the platform's scalability, maintainability, and analytical
depth. They are not required for production use but become worthwhile once the
platform is running daily.

---

### P4-1 — Component-based frontend

Replace the single `index.html` with a lightweight component framework (Preact
or Alpine.js). The current file is approaching 500 lines; Phase 2 additions
will triple that. A component model with a simple Vite build makes the UI
maintainable. See ENHANCEMENTS.md ENH-031.

### P4-2 — Server-Sent Events for live candidate feed

Replace polling with SSE: new PASS/WATCH verdicts push to connected clients
in real time. Spring's `SseEmitter` is sufficient without adding a reactive
stack. See ENHANCEMENTS.md ENH-032.

### P4-3 — OpenAPI / Swagger documentation

Add `springdoc-openapi-starter-webmvc-ui`. The existing controller Javadoc
produces a largely complete spec with minimal additional annotation. Expose
`/swagger-ui.html` in non-production profiles. See ENHANCEMENTS.md ENH-033.

### P4-4 — Scheduled intraday market-state snapshots

A cron job that fetches intraday bars for each watch-list symbol every 15
minutes and writes a regime classification to a `market_state_snapshot` table.
Enables real-time regime monitoring without manual triggers. See ENHANCEMENTS.md
ENH-035.

### P4-5 — Additional SEC ingestion sources

13D/G filings, offering circulars, XBRL data, and RSS firehose ingestion are
explicitly deferred feature design. These require storage, deduplication, and
parsing strategy decisions before entering the default pipeline.

### P4-6 — Calibration report persistence

Store each `CalibrationReport` snapshot in a `calibration_report` table with
a `generatedAt` timestamp. This enables long-horizon drift trending rather than
point-in-time snapshots. See ENHANCEMENTS.md ENH-026.

---

## Work item summary by phase

| Phase | Items | Focus |
|---|---|---|
| **Phase 0** | P0-1 through P0-6 | Blockers: persistent DB, auth, CORS, transactions |
| **Phase 1** | P1-1 through P1-9 | Daily reliability: validation, tests, logging, core UI |
| **Phase 2** | P2-1 through P2-10 | Full review surface: all backend features in UI |
| **Phase 3** | P3-1 through P3-7 | Live execution: Schwab orders, gated and audited |
| **Phase 4** | P4-1 through P4-6 | Long-horizon: frontend, SSE, OpenAPI, more ingestion |
