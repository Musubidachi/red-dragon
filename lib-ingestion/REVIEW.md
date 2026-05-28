# lib-ingestion Review

Last updated: 2026-05-27

This review summarizes the current state after the SEC hardening and taxonomy
fix passes. Older line-by-line audit detail has been compressed so open work is
not hidden under fixed findings.

## Current Open Work

| Priority | Issue | Status | Impact | Next action |
| --- | --- | --- | --- | --- |
| Medium | SEC RSS / firehose and LLM ticker scheduler | Deferred | Design docs exist, but the code surface is not present. | Keep docs marked as future work until packages, config, migrations, and source types exist. |

## Implemented Surface

Implemented sources today:

* Manual candidate ingestion through `ManualCandidateIngestionService`.
* SEC EDGAR submissions ingestion through `services/sec/SecIngestionService`
  by CIK or by ticker.
* Ticker-to-CIK lookup with configurable TTL and fail-stale refresh after the
  first successful load.
* CIK-to-tickers reverse lookup that preserves all SEC-listed share-class
  tickers in source order and exposes the first SEC-listed ticker as the
  preferred representative.
* Primary-document body fetches through `SecFilingBodyClient`, reusing
  `SecHttpClient` rate limits, user-agent headers, timeouts, and retries.
* Form 4 ownership XML parsing for issuer identity, reporting owners, and
  non-derivative transactions, covered by fixtures for priced transactions and
  no-price gift transactions.
* Opt-in Form 4 body-derived candidate creation through
  `SecForm4BodySignalService`, with stable body-specific candidate/source IDs
  and no network call for non-Form-4 filings.
* SEC HTTP client with configured user agent validation, timeouts, retry/backoff,
  and bounded rate limiting.
* CIK format utilities for zero-padding, URL archive paths, accession-number
  normalization, and path-segment encoding.
* SEC submission flattening with in-scope form filtering, amendment and `424B`
  variant handling, exchange-aware ticker selection, OTC filtering, and
  tickerless filing drops.
* 8-K item mapping with M&A, disclosure, severe, dilution, contract, earnings,
  governance, and residual categories mapped into current domain catalyst
  types.
* Acceptance timestamp parsing from SEC Eastern time to `Instant` for earlyness
  scoring.
* Manual candidate validation, deterministic manual candidate IDs, and clock
  injection for repeatable tests.

## Historical Fixes

The following earlier findings are fixed and should not be reintroduced as open
work:

| Area | Fixed outcome |
| --- | --- |
| SEC taxonomy | M&A and disclosure events no longer collapse into generic contract/filing behavior; severe and dilution buckets were extended. |
| SEC HTTP behavior | User-agent validation, timeouts, retry/backoff, and token-bucket rate limiting were added. |
| CIK and URL handling | CIK padding/stripping, accession normalization, and URL encoding now live in `CikFormats`. |
| CIK lookup wiring | `CikLookupService` now refreshes by TTL, fails stale after a successful load, and is wired into `SecIngestionService.processByTicker`. |
| Ticker handling | Submissions extraction uses exchange data, filters OTC names, keeps missing exchange data defensively, and drops tickerless filings. |
| Filing recency | `acceptanceDateTime` is parsed and used before falling back to filing-date midnight. |
| Manual ingestion | Required score validation, deterministic IDs, and injectable time are implemented. |
| Form matching | `/A` amendments and `424B1` through `424B5` are recognized without broad prefix collisions. |
| 8-K hygiene | Standalone `9.01` is stripped before classification, mapper priority is documented, and unreachable default scoring was removed. |
| SEC package docs | The package README now links data/config classes correctly and includes `CikFormats`, `CikLookupService`, and `SecFilingScoringHeuristics` in the reading order. |
| CIK reverse lookup | `CikLookupService` now builds ticker-to-CIK and CIK-to-tickers views from the same TTL-managed SEC company ticker payload. |
| Jackson wiring | SEC JSON services require an injected `ObjectMapper`, and app-side wiring passes the shared Spring mapper. |
| Form 4 body signal slice | `SecFilingBodyClient`, `Form4OwnershipXmlParser`, and `SecForm4BodySignalService` fetch, parse, and emit opt-in body-derived candidates with fixture-backed tests. |

## Deferred Design Notes

These documents remain forward-looking and should not be described as
implemented until code exists:

* `LLM_TICKER_SCHEDULER.md`
* SEC RSS firehose / Loop B in `SEC_API.md` and `SEC_IMPLEMENTATION.md`
* document caching to local filesystem
* EFTS full-text search
* daily index backfill
* Form 4 derivative tables, holdings-only rows, footnotes, and final signal
  semantics
* full filing body parsers for 13D/G, S-1/S-3/424B, and XBRL

Completed RD-M2 Form 4 body slice: `SecFilingBodyClient` fetches the existing
`SecFiling.primaryDocumentUrl()` through `SecHttpClient`,
`Form4OwnershipXmlParser` parses Form 4 ownership XML with fixture-backed
tests, and `SecForm4BodySignalService` emits opt-in `TradeCandidate` signals
for Form 4 / Form 4-A filings. The default submissions pipeline remains
metadata-driven; callers must opt in before spending filing-body fetches.

Root tracking: [../ISSUES.md](../ISSUES.md).
