# red-dragon Enhancement Proposals

Last updated: 2026-05-27

This document now tracks enhancement work that still remains after the first
dashboard surfacing pass and the related backend cleanup pass.

Completed enhancements from the latest pass are listed at the bottom so the
remaining backlog stays readable.

---

## Part 1 - Remaining UI surfacing work

These items are still product-visible gaps.

---

### ENH-003 - Candidate detail panel (drill-down on card click)
**API:** `GET /api/candidates/id/{candidateId}`

Candidate cards are still mostly summary-only. Build the drill-down surface for
company metadata, market snapshots, analytics snapshots, full reasons, notes,
and overrides.

---

### ENH-004 - Trader notes on candidate cards
**API:** `GET /api/candidates/{id}/notes`, `POST /api/candidates/{id}/notes`

The backend supports notes, but the dashboard still does not expose note
reading/writing in the review flow.

---

### ENH-005 - Verdict override action
**API:** `POST /api/verdicts/{id}/override`

The override mechanism is implemented server-side, but the dashboard still
lacks the action UI and summary badge behavior.

---

### ENH-007 - Regime history per symbol
**API:** `GET /api/regime/history?lookbackHours=48`

Recent regime data still is not surfaced symbol-by-symbol in the review
experience.

---

### ENH-008 - Symbol-level performance inline on cards
**API:** `GET /api/calibration/summary/{symbol}`

Per-symbol calibration performance is still not shown inline on candidate
cards.

---

### ENH-010 - Ticker search + analysis panel
**API:** `GET /api/analysis/{ticker}?lookbackDays=60&profile=STANDARD`

The on-demand ticker analysis flow is still not exposed in the dashboard.

---

### ENH-011 - Backtest runner + run browser
**API:** `POST /api/backtest`, `GET /api/backtest/runs`,
`GET /api/backtest/results/{runId}`

The backtest harness persists and reports results, but it still lacks a UI.

---

### ENH-013 - Live quote on candidate cards
**API:** `GET /api/market-data/{symbol}/quote`

Candidate cards still do not show live quote context.

---

### ENH-014 - Daily price chart on candidate detail
**API:** `GET /api/market-data/{symbol}/daily?from=...&to=...`

Stored daily bars are still not visualized in the dashboard.

---

### ENH-016 - Trade history import panel
**API:** `POST /api/history/import`, `GET /api/history/imports`,
`GET /api/history/trades`

Trade-history import remains API-only and still needs UI surfacing.

---

### ENH-017 - SEC watch-list trigger button
**API:** `GET /api/pipeline/sec/watch-list?ciks=...&lookbackDays=30`

The watch-list runner remains API-only.

---

### ENH-018 - Validation profile picker
**API:** `GET /api/validation/profiles`

Validation profiles exist in the backend but are still invisible in the
dashboard flow.

---

### ENH-019 - Calibration report (L8 framework health) panel
**API:** `GET /api/calibration`

Framework-health output is still not surfaced in the dashboard.

---

### ENH-020 - Market state classifier tool
**API:** `POST /api/market-state/classify`

The market-state classifier remains API-only.

---

## Part 2 - Remaining backend improvements

These still require backend work.

---

### ENH-022 - Consolidate remaining calibration summary surface
**Module:** `app`

The misleading scalar payload shape is fixed, but the calibration summary API
surface is still large. Decide whether to deprecate or remove the many scalar
`/api/calibration/summary/**` endpoints in favor of the fuller summary
endpoints.

---

### ENH-025 - Spring Security with API-key authentication
**Module:** `app`

Still open. This remains aligned with `ISS-002`.

---

### ENH-026 - Persist calibration report outcomes from `POST /api/calibration`
**Module:** `app`

Calibration reports are still computed but not stored historically.

---

### ENH-030 - Per-symbol calibration data feed into candidate review
**Module:** `app`

Candidate cards still do not batch-load symbol performance. This likely wants a
batch summary endpoint before the UI pass.

---

## Part 3 - Architectural / longer-horizon items

These remain deferred and are still useful to track.

---

### ENH-031 - Replace single-file `index.html` with a component-based frontend

The dashboard has grown again and now carries stats, candidate review,
opportunity quality, performance tables, outcomes journal, and the exit-signal
tool in one file. Further UI growth should probably not continue indefinitely in
one static page.

---

### ENH-032 - Server-Sent Events (SSE) for live candidate feed

Still deferred.

---

### ENH-033 - OpenAPI / Swagger documentation

Still deferred.

---

### ENH-034 - lib-execution live Schwab order integration

Still deferred and still subject to explicit approval and fail-closed controls.

---

### ENH-035 - Scheduled intraday market-state snapshot

Still deferred pending persistence and scheduler design.

---

## Completed In The Latest Pass

These enhancements were completed and are no longer part of the remaining
backlog above:

- `ENH-001` verdict distribution stats bar
- `ENH-002` REJECT filter pill
- `ENH-006` opportunity quality section
- `ENH-009` top / worst performers panel
- `ENH-012` pipeline status widget
- `ENH-015` calibration outcomes journal with CSV export
- `ENH-021` CORS config wired to property
- `ENH-023` candidate listing DTOs
- `ENH-024` bean validation on key request models
- `ENH-027` backtest candidate persistence hardening
- `ENH-028` SEC scheduler empty-CIK startup warning
- `ENH-029` lazy-loaded verdict reasons with targeted repository fetches
