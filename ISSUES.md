# red-dragon Issue Index

Last updated: 2026-05-27

This index tracks current actionable issues that still remain after the latest
code and dashboard cleanup pass. Fixed historical findings are intentionally
omitted unless they leave a follow-up.

---

## High Priority

### ISS-002 - No authentication or authorization on any endpoint
**Module:** `app` · **Scope:** all controllers

There is still no Spring Security configuration. Every endpoint, including
destructive routes such as `DELETE /api/calibration/outcomes`,
`POST /api/verdicts/{id}/override`, and `POST /api/calibration`, is publicly
reachable. Schwab OAuth routes are also unauthenticated.

**Why it remains:** This was explicitly deferred while addressing the non-security
Sonnet issue set.

**Fix:** Add a minimal Spring Security configuration, at minimum requiring a
static API key via `Authorization: Bearer <token>` and wiring it so CORS
pre-flight requests continue to work.

---

## Medium Priority

### ISS-019 - Controller-layer integration coverage is still incomplete
**Module:** `app` · **Directory:** `src/test/java/dev/reddragon/app/`

The recent pass added focused controller tests for candidate listing,
calibration summaries, and manual-review validation, but the controller test
surface is still incomplete. In particular, dedicated coverage is still thin or
missing for the candidate review history/detail flows, trade-history import,
ticker analysis, and duplicate-skip behavior around pipeline orchestration.

**What was completed already:**
- `CandidateControllerTest`
- `CalibrationControllerTest`
- `ManualReviewControllerValidationTest`
- existing app context and backtest wiring coverage

**Fix:** Continue adding `@WebMvcTest` or `@SpringBootTest` coverage by
production ownership area instead of one broad catch-all pass. Prioritize:
candidate history/detail, trade-history import, ticker analysis, and any
controller path that now depends on the new validation/error envelope.

---

## Notes

The following Sonnet items were completed and are no longer tracked as open
issue debt in this file:

- `ISS-001` CORS property wiring
- `ISS-003` backtest transactional boundary
- `ISS-004` bounded regime-history query
- `ISS-005` `/api/market-structure` path fix
- `ISS-006` deprecated verdict blob fields hidden from JSON
- `ISS-007` honest candidate history limit handling
- `ISS-008` injected market-structure dependencies
- `ISS-009` dedicated scalar calibration response types
- `ISS-010` bean validation on key request models
- `ISS-011` `CandidateController` response DTOs
- `ISS-012` structured validation-error handling
- `ISS-013` duplicate-skip metric and logging
- `ISS-014` removal of misleading trade-history heuristics
- `ISS-015` duplicate V1 `.bak` migration cleanup
- `ISS-016` SEC scheduler empty-CIK startup warning
- `ISS-017` calibration controller field-order cleanup
- `ISS-018` lazy-loaded verdict reasons with targeted fetches
- `ISS-020` `SERVER_ADDRESS` requirement documented in the root README
