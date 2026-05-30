# red-dragon Enhancement Proposals

Last updated: 2026-05-28

This document now tracks enhancement work that still remains after the first
dashboard surfacing pass and the related backend cleanup pass.

Completed enhancements from the latest pass are listed at the bottom so the
remaining backlog stays readable.

---

## Part 2 - Remaining backend improvements

These still require backend work.

---

## Part 3 - Architectural / longer-horizon items

These remain deferred and are still useful to track.

---

### ENH-034 - lib-execution live Schwab order integration

Still deferred and still subject to explicit approval and fail-closed controls.

---

## Completed In The Latest Pass

These enhancements were completed and are no longer part of the remaining
backlog above:

- `ENH-001` verdict distribution stats bar
- `ENH-002` REJECT filter pill
- `ENH-003` candidate detail panel with company metadata, full reasons,
  market/analytics snapshots, notes, and override history
- `ENH-004` trader note badges plus note read/write controls in the candidate
  detail flow
- `ENH-005` verdict override action UI with card-level override badge behavior
- `ENH-006` opportunity quality section
- `ENH-007` symbol-scoped regime history in the review experience
- `ENH-008` symbol-level calibration performance inline on candidate cards
- `ENH-009` top / worst performers panel
- `ENH-010` ticker search and analysis panel
- `ENH-011` backtest runner and stored run browser
- `ENH-012` pipeline status widget
- `ENH-013` live quote context on candidate cards with unavailable-provider
  state
- `ENH-014` daily price chart on candidate detail with stored-bar fallback
- `ENH-015` calibration outcomes journal with CSV export
- `ENH-016` trade-history import panel with import batch and recent-trade views
- `ENH-017` SEC watch-list trigger with profile, lookback, and result table
- `ENH-018` validation profile picker in the ticker analysis flow
- `ENH-019` calibration report panel for L8 framework health
- `ENH-020` market-state classifier tool with sample intraday-bar payload
- `ENH-021` CORS config wired to property
- `ENH-022` consolidated calibration summary detail endpoints with deprecation
  headers on scalar summary endpoints
- `ENH-023` candidate listing DTOs
- `ENH-024` bean validation on key request models
- `ENH-025` Spring Security with API-key authentication
- `ENH-026` persisted calibration report history from calibration/backtest
  analysis plus `GET /api/calibration/reports`
- `ENH-027` backtest candidate persistence hardening
- `ENH-028` SEC scheduler empty-CIK startup warning
- `ENH-029` lazy-loaded verdict reasons with targeted repository fetches
- `ENH-030` batch per-symbol calibration summary feed consumed by candidate
  card context loading
- `ENH-031` static dashboard split from single-file `index.html` into HTML,
  stylesheet, and module script assets
- `ENH-032` Server-Sent Events stream for live candidate feed refreshes
- `ENH-033` OpenAPI JSON and Swagger UI via Springdoc
- `ENH-035` scheduled intraday market-state snapshots with persistence and
  recent snapshot API
