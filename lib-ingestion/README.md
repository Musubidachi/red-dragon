# lib-ingestion

> **MD layers:** L1 (Opportunity Discovery) and the candidate half of L2 (Data
> Ingestion). See [ARCHITECTURE.md](../ARCHITECTURE.md).

`lib-ingestion` turns outside information into normalized `TradeCandidate`
objects that the rest of Red Dragon can evaluate.

## Current Implementation

Implemented sources:

* Manual candidate ingestion through `ManualCandidateIngestionService`.
* SEC EDGAR submissions ingestion through `services/sec/SecIngestionService`.

The SEC implementation currently fetches recent company submissions by CIK,
filters in-scope forms, flattens filing metadata, maps 8-K item codes, and
builds scored `TradeCandidate` records.

## Responsibilities

* Pull raw candidate inputs from external sources.
* Normalize source-specific payloads into internal candidate objects.
* Preserve enough source metadata for auditability and review.
* Keep source adapters isolated from the rest of the pipeline.
* Fail safely when a source is unavailable or malformed.

## Non-Responsibilities

This library should not classify market regime, fetch OHLCV bars, persist
candidates directly, place orders, or know anything about portfolio state.

## Current Package Layout

```text
lib-ingestion/src/main/java/dev/reddragon/ingestion
    models/       TradeCandidate, SourceType, CandidateCatalystType, SEC models
    services/     manual ingestion service
    services/sec/ SEC submissions client, rate limiter, filing extractor, candidate builder
    config/       SecApiProperties
    utilities/    text helpers
```

## Planned Or Deferred

* Generic source SPI.
* News/RSS, scanner, and macro adapters.
* SEC RSS firehose polling.
* SEC CIK/ticker cache from `company_tickers*.json`.
* Full Form 4, 13D/G, offering body, and XBRL parsing.

## Testing Expectations

Tests should cover parser behavior, malformed or missing fields, deterministic
mapping from raw payload to normalized candidate, duplicate behavior, and source
adapter failures. Prefer realistic fixtures where useful.
