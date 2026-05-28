# red-dragon — Production Readiness

Last updated: 2026-05-27

This document describes what stands between the current state of red-dragon and
a version that can be run reliably as a personal production tool. It is written
against the system described in [README.md](README.md), [ARCHITECTURE.md](ARCHITECTURE.md),
and the current source code.

The platform's stated purpose is narrow and deliberate: it augments discretionary
trading decisions; it does not place orders autonomously. "Production ready" for
red-dragon therefore means: **trustworthy enough that you can rely on its verdicts,
stable enough that it stays running unattended, and safe enough that a misconfiguration
cannot cause financial damage.**

---

## What is already solid

Before listing gaps, it is worth being explicit about what the platform does well
today. Most production risk is operational and security-scoped, not algorithmic:

- The core pipeline (ingestion → enrichment → analytics → validation) is
  deterministic, well-tested, and idempotent.
- All 8 MD layers are implemented. Scorers are pure functions with no I/O or
  mutable state.
- Flyway migrations (V1–V16) cover 15 tables with audit timestamps, optimistic
  locking, normalized reason codes, and AES-GCM token encryption.
- The `lib-execution` dry-run module is implemented and fail-closed; live
  order placement is deliberately not wired.
- Hard gates in `HardGateEvaluator` collect all failures before returning
  (non-fail-fast), so the trader sees every rejection reason in one pass.
- Four named validation profiles (CONSERVATIVE, STANDARD, AGGRESSIVE,
  CONCENTRATION_REVIEW) allow risk posture adjustment without code changes.
- Schwab OAuth three-legged flow, token refresh, and AES-encrypted persistence
  are implemented.
- Micrometer metrics and Spring Actuator are wired; SLO buckets are configured
  for HTTP and pipeline latency.

---

## Gap 1 — Security (Blocker)

The platform currently has **no authentication or authorization layer**. Every
endpoint is world-accessible on whatever network interface the server binds to.

### 1.1 No API authentication

`DELETE /api/calibration/outcomes`, `POST /api/verdicts/{id}/override`,
`POST /api/calibration` (submit trade outcomes), and all Schwab OAuth endpoints
are reachable without any credential. An adversary on the same network can
delete all calibration history or inject fabricated trade outcomes.

**Required:** Add Spring Security. For a personal tool, a static API key
(`Authorization: Bearer <token>` header checked by a `OncePerRequestFilter`) is
sufficient. Exempt only `/health`, `/actuator/health`, and
`/api/schwab/oauth/callback` (Schwab redirects here externally and cannot carry
a bearer token).

### 1.2 CORS wildcard not wired to environment variable

`WebConfiguration.corsConfigurer()` hardcodes `allowedOriginPatterns("*")`
regardless of the `RED_DRAGON_CORS_ALLOWED_ORIGINS` environment variable defined
in `application.yml`. In any deployment where the server is accessible beyond
localhost, this allows any origin to call mutating endpoints.

**Required:** Inject the property into `WebConfiguration` and pass it to
`allowedOriginPatterns(...)`. See ISSUES.md ISS-001.

### 1.3 Secrets hygiene

- `SCHWAB_ACCESS_TOKEN` (static fallback) and `OPENAI_API_KEY` are read directly
  from environment. Ensure they are never written to logs. The existing
  `RequestLoggingFilter` should mask any header containing `Authorization` or
  `Token`.
- The Schwab refresh token is encrypted at rest (AES-GCM) — this is correct.
  Confirm the encryption key (`SCHWAB_REFRESH_KEY` or equivalent) is documented
  and its absence causes a startup failure rather than a silent fallback to
  plaintext.
- Historical plaintext Schwab token rows (pre-V15 migration) should be purged or
  re-saved through the encrypting converter before live use.

---

## Gap 2 — Data persistence (Blocker for anything beyond local dev)

### 2.1 H2 in-memory default database

`application.yml` defaults to `jdbc:h2:mem:reddragon`. All data is lost on every
restart. This is correct for development but must be replaced before the platform
is used in any session that spans multiple days or restarts.

**Required:** Configure a persistent database. PostgreSQL is the natural choice
(the H2 config uses `MODE=PostgreSQL` already). Provide:
- `RED_DRAGON_DB_URL` pointing to a local or hosted Postgres instance
- `RED_DRAGON_DB_USERNAME` and `RED_DRAGON_DB_PASSWORD`
- `RED_DRAGON_DB_DRIVER=org.postgresql.Driver`
- The PostgreSQL JDBC driver on the classpath (`org.postgresql:postgresql`)

For a single-machine deployment, a local Postgres instance managed by Docker
Compose is the simplest approach.

### 2.2 No database backup or recovery procedure

There is no documented backup strategy for the Postgres database. Calibration
outcomes, candidate history, analytics snapshots, and trade history records are
the platform's institutional memory. Losing them means losing all calibration
signal.

**Required:** Document (and ideally automate) a periodic `pg_dump` job. For a
personal tool a daily dump to a local directory with a cron job is sufficient.

### 2.3 No Flyway baseline for existing databases

If the database already has data from H2 and is being migrated to Postgres, the
Flyway migration history will not transfer. A baseline migration procedure must
be documented.

---

## Gap 3 — Operational reliability

### 3.1 No deployment configuration

There is no `Dockerfile`, `docker-compose.yml`, or systemd unit file. The app
can only be started manually with `mvn spring-boot:run` or by running the fat
jar directly. There is no documented start/stop/restart procedure.

**Required:** At minimum, a `docker-compose.yml` that spins up:
- A Postgres container (with a named volume for persistence)
- The red-dragon `app` container (with all required env vars documented)
- Port mapping for `8080`

The `SERVER_ADDRESS` currently defaults to `127.0.0.1` (loopback only). For
any networked deployment this must be set to `0.0.0.0`. Document this.

### 3.2 No process supervision

If the JVM crashes or the server reboots, the app does not restart. A production
deployment needs either a Docker restart policy (`restart: unless-stopped`) or a
systemd unit with `Restart=on-failure`.

### 3.3 `BacktestController.runBacktest` is not transactional

If `calibrationOutcomeService.appendBacktestOutcomes(...)` throws after
`backtestResultRepository.saveAll(...)` completes, backtest results are persisted
but calibration outcomes are not. The stores are left inconsistent with no
rollback. See ISSUES.md ISS-003.

**Required:** Add `@Transactional` to `runBacktest`.

### 3.4 `RegimeHistoryController` loads all rows in memory

The unfiltered `GET /api/regime/history` path calls `findAll()` on the
`analytics_snapshot` table and streams/sorts in Java. With any meaningful volume
of pipeline runs this will OOM or time out. See ISSUES.md ISS-004.

**Required:** Replace with a bounded repository query before enabling the
SEC scheduler or any high-volume ingestion.

### 3.5 No log aggregation or alerting

Application logs go to stdout only. There is no facility to alert on:
- Pipeline errors or high rejection rates
- Schwab token expiry (the 7-day refresh window is operationally critical)
- JVM memory pressure or OOM

**Required (minimum):** Log to a rolling file with `logback.xml` so logs survive
restarts. Add a `log.warn(...)` in `SchwabTokenRefresher` when the refresh token
has fewer than 24 hours remaining.

---

## Gap 4 — Input validation and API robustness

### 4.1 No Bean Validation on request models

`ManualReviewRequest`, `PipelineReviewRequest`, and `CalibrationOutcomeSampleRequest`
accept any numeric value including those outside `[0.0, 1.0]`. An omitted score
field defaults to `0.0` and silently flows through the pipeline as a valid low
score rather than triggering an error. See ISSUES.md ISS-010.

**Required:** Annotate score fields with `@DecimalMin("0.0")` / `@DecimalMax("1.0")`,
string fields with `@NotBlank`, and add `@Valid` to `@RequestBody` parameters.
Handle `MethodArgumentNotValidException` in `GlobalExceptionHandler`.

### 4.2 `GlobalExceptionHandler` does not handle Bean Validation exceptions

`ConstraintViolationException` and `MethodArgumentNotValidException` bypass the
`IllegalArgumentException` handler and fall through to the generic 500 handler,
returning an opaque error rather than a structured 400. See ISSUES.md ISS-012.

### 4.3 `CandidateController` leaks JPA entity in API response

`GET /api/candidates` returns `Page<CandidateEntity>` directly. Every schema
migration is a breaking API change for any client parsing the response. See
ISSUES.md ISS-011.

### 4.4 `MarketStructureController` path is outside CORS policy

`@RequestMapping("/market-structure")` does not match the `/api/**` CORS mapping.
Browser clients cannot call this endpoint. See ISSUES.md ISS-005.

---

## Gap 5 — Testing

### 5.1 No controller integration tests

Only three test files exist in `app/src/test/`: a context-load test, a backtest
controller wiring test, and a calibration service unit test. The following have
no test coverage at all:

- `CandidatePipelineOrchestrator`
- `TickerAnalysisService`
- `ManualReviewController`
- `PipelineReviewController`
- `CalibrationController`
- `TradeHistoryImportController`
- `CandidateHistoryController`
- `VerdictOverrideController`
- `TraderNoteController`

See ISSUES.md ISS-019.

**Required:** At minimum, add `@WebMvcTest` or `@SpringBootTest` + `MockMvc`
tests for each controller covering: happy path, missing required fields (400),
not-found paths (404), and the duplicate-skip behavior in the pipeline orchestrator.

### 5.2 No end-to-end test of the SEC scheduler path

The SEC watch-list scheduler (`SecWatchListScheduler`) runs on a cron, fetches
SEC filings from EDGAR, and pushes candidates through the pipeline. This path
has no integration test. A misconfiguration in the CIK list or SEC HTTP client
would silently produce zero candidates with no observable error.

### 5.3 No load or soak test

The pipeline has no performance baseline. Under repeated SEC scheduler runs (e.g.,
`lookbackDays=90`, 20 CIKs) the `analytics_snapshot` table will grow quickly and
the unguarded `findAll()` in `RegimeHistoryController` will degrade.

---

## Gap 6 — UI completeness

The web UI at `index.html` exposes only a fraction of the platform's capabilities.
A meaningful set of backend endpoints are implemented, tested, and returning data,
but are invisible to the user. The most critical for day-to-day use:

| Missing UI feature | Powering endpoint(s) |
|---|---|
| Reject tab (see what was filtered and why) | `GET /api/review/candidates?verdicts=REJECT` |
| Candidate detail drill-down (full scores, notes, overrides) | `GET /api/candidates/id/{candidateId}` |
| Ticker search / on-demand analysis | `GET /api/analysis/{ticker}` |
| Verdict distribution stats (pipeline throughput) | `GET /api/verdicts/stats` |
| Trader notes (add/view annotations per candidate) | `POST/GET /api/candidates/{id}/notes` |
| Verdict override button | `POST /api/verdicts/{id}/override` |
| Calibration outcomes journal + CSV export | `GET /api/calibration/outcomes/page`, `/export` |
| Top/worst historical performers | `GET /api/calibration/outcomes/top|worst` |
| Trade history import and view | `POST /api/history/import`, `GET /api/history/trades` |
| Backtest runner and run browser | `POST /api/backtest`, `GET /api/backtest/runs` |
| Pipeline status indicator | `GET /api/pipeline/status` |

Full detail on each enhancement is in [ENHANCEMENTS.md](ENHANCEMENTS.md) (ENH-001
through ENH-020).

---

## Gap 7 — Schwab live execution (Intentionally deferred)

As documented in [lib-execution/SCHWAB_EXECUTION.md](lib-execution/SCHWAB_EXECUTION.md),
live broker order placement is deliberately not implemented. The dry-run module
is built and fail-closed. The following remain outstanding before live execution
could be enabled:

1. Schwab developer portal approval and linked brokerage account
2. `SchwabHttpClient` for `/trader/v1/...` endpoints
3. Read-only account/position/order reads (implement and validate first)
4. Persistent broker audit log (`broker_call_log` table)
5. `executionMode=dry-run|live` config with dry-run as the default
6. App-level human confirmation gate before any `placeOrder` call
7. Idempotency key reconciliation on network failure (never auto-retry order placement)
8. Refresh-token expiry warning (T-24h alert; 7-day window is operationally tight)

This is documented as future work. Do not enable live trading until every item
on this list is verified end-to-end. The financial risk of a misconfigured or
partially implemented execution layer is unacceptable.

---

## Gap 8 — Configuration hygiene

### 8.1 SEC scheduler silently no-ops with empty CIK list

If `RED_DRAGON_SEC_SCHEDULER_ENABLED=true` without `RED_DRAGON_SEC_SCHEDULER_CIKS`
set, the scheduler fires, runs against an empty list, and logs nothing useful.
See ISSUES.md ISS-016.

### 8.2 Sample data enabled by default

`RED_DRAGON_SAMPLE_DATA_ENABLED` defaults to `true` via `H2SampleDataLoader`.
In a production database this will insert synthetic records on every startup.
This must be set to `false` in any non-development environment.

### 8.3 `server.address` defaults to loopback

`server.address` defaults to `127.0.0.1`. The app is unreachable from Docker
host networking or any remote client unless `SERVER_ADDRESS=0.0.0.0` is set.
This is a correct security default for development but must be documented for
any deployment. See ISSUES.md ISS-020.

### 8.4 No documented required environment variables

There is no single reference document listing all environment variables required
for a production deployment, their default values, and which are mandatory vs.
optional. An operator setting up the system must reverse-engineer this from
`application.yml`.

**Required:** Add a section to `README.md` (or a dedicated `DEPLOYMENT.md`) that
lists every `${...}` variable, its purpose, default, and whether it is required.

---

## Summary checklist

### Blockers — must be resolved before any production use

- [ ] Add API authentication (Spring Security + API key)
- [ ] Switch to a persistent database (PostgreSQL)
- [ ] Set `RED_DRAGON_SAMPLE_DATA_ENABLED=false`
- [ ] Wire CORS config to the environment variable
- [ ] Add `@Transactional` to `BacktestController.runBacktest`
- [ ] Fix `RegimeHistoryController` in-memory `findAll()` path

### High — resolve before regular daily use

- [ ] Add Bean Validation to request models + handle in `GlobalExceptionHandler`
- [ ] Fix `MarketStructureController` path to `/api/market-structure`
- [ ] Add `@JsonIgnore` to deprecated `ValidationVerdictEntity` fields
- [ ] Add controller integration tests (at minimum happy path + 400 cases)
- [ ] Document all environment variables in `README.md` or `DEPLOYMENT.md`
- [ ] Log Schwab refresh-token expiry warning at T-24h
- [ ] Configure log rolling so logs survive restarts
- [ ] Add a `docker-compose.yml` with Postgres + app container

### Standard — resolve before sharing or extended use

- [ ] Add REJECT tab to the UI
- [ ] Add candidate detail drill-down to the UI
- [ ] Add ticker search / analysis panel to the UI
- [ ] Purge or re-encrypt historical plaintext Schwab token rows
- [ ] Fix SEC scheduler empty-CIK silent no-op with a startup warning
- [ ] Add response DTOs to `CandidateController` (stop leaking entity)
- [ ] Add `CandidatePipelineOrchestrator` duplicate-skip metric/log
