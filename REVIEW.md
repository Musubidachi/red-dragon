# red-dragon Review

Last updated: 2026-05-27

This is the repo-wide current work-remains summary. Module review files contain
the detailed local context; [ISSUES.md](ISSUES.md) is the task index and
[ENHANCEMENTS.md](ENHANCEMENTS.md) tracks the remaining product/UI backlog.

## Current Open Work

Current repo-wide work is now narrow and explicit:

1. Security is still missing.
   The app still has no authentication/authorization layer. This remains the
   top open issue in [ISSUES.md](ISSUES.md).
2. Controller coverage is improved but not complete.
   Focused tests were added for candidate listing, calibration summaries, and
   manual-review validation, but broader controller integration coverage still
   needs to be filled in by ownership area.
3. The dashboard enhancement backlog remains active.
   The first dashboard surfacing slice landed, but candidate drill-down,
   operator tools, and remaining framework-health UI still need to be built.

## What Changed Recently

The latest code pass materially reduced the repo-wide issue list:

- CORS property wiring now follows `application.yml`
- backtest writes now run inside a transaction boundary
- market-structure and market-state controllers use injected dependencies
- regime-history queries are bounded at the repository layer
- candidate history limit handling is now honest and pageable
- trade-history import no longer persists misleading heuristic strategy/state values
- validation verdict legacy blob fields are hidden from JSON and reasons are lazy-loaded
- candidate duplicate skips now emit metrics/logging
- calibration scalar endpoints now use dedicated response types
- key request models now use Bean Validation with structured 400 responses
- candidate listing now returns a DTO instead of raw JPA entities
- the root README now documents the `SERVER_ADDRESS` bind requirement
- the test runtime now works under Java 24 via the Surefire Byte Buddy flag

## Current Shape

The pipeline is functional as a candidate-review system: `app` can run manual
and SEC-sourced candidates through market-data enrichment, analytics,
validation, review, persistence, calibration, backtest, and a growing static
dashboard surface. The implemented app still stops before live broker
execution.

`lib-execution` models provider-neutral dry-run account, position, order,
cancellation, and fill lifecycle behavior in the Maven reactor, but live
Schwab trading still fails closed and remains deferred pending explicit product
and operations choices.

## Remaining Product/Operations Work

The highest-value remaining work is no longer broad code hygiene; it is
product-facing completion work:

- secure the application surface
- finish the dashboard drill-down and operator workflows
- decide whether to keep extending the single-file dashboard or replace it with
  a component-based frontend
- continue deferred ingestion/product items such as non-Form-4 SEC body parsers,
  SEC RSS/firehose ingestion, LLM ticker scheduling, and live Schwab execution
  only when the corresponding storage, operational, and compliance decisions are
  explicit
