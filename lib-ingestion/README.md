# lib-ingestion

`lib-ingestion` is responsible for turning outside information into normalized trade candidates that the rest of Red Dragon can evaluate.

This module should answer one question:

> What new market-relevant candidates should the system consider?

It should not decide whether a candidate is attractive, safe, asymmetric, or valid. Those decisions belong downstream.

## Responsibilities

- Pull raw candidate inputs from external sources.
- Normalize source-specific payloads into internal candidate objects.
- Preserve enough source metadata for auditability and review.
- Keep source adapters isolated from the rest of the pipeline.
- Fail safely when a source is unavailable or malformed.

Potential sources include:

- SEC EDGAR filings
- News or RSS feeds
- Market scanners
- Macro feeds
- Manually supplied candidate lists

## Non-responsibilities

This library should not:

- Score trade quality.
- Classify market regime.
- Fetch historical OHLCV bars for feature calculation.
- Persist candidates directly unless routed through persistence contracts.
- Know anything about portfolio state, open positions, or order execution.

## Expected flow

```text
external source
    -> source adapter
    -> raw source payload
    -> parser / mapper
    -> normalized candidate
    -> downstream enrichment
```

## Design guidance

### Keep adapters thin

Each source adapter should focus on retrieving and minimally parsing that source. Shared normalization logic should live outside the adapter when possible.

### Prefer deterministic sources first

SEC EDGAR is the best first ingestion target because it is structured, free, and easier to validate than social/news sentiment feeds.

### Preserve provenance

Every candidate should carry enough source context to explain why it entered the system:

- source name
- source timestamp
- source URL or identifier
- raw symbol / company identifier
- normalized ticker, if available
- trigger type, such as filing, headline, scanner result, or macro event

### Avoid premature AI scoring

AI-generated summaries may be useful later, but this module should first prove that it can reliably produce candidates from deterministic sources.

## Suggested package layout

```text
lib-ingestion
└── src/main/java/dev/reddragon/ingestion
    ├── candidate       # normalized candidate contracts
    ├── source          # source interfaces and common source metadata
    ├── sec             # SEC EDGAR adapter and parser
    ├── news            # RSS/news adapters, later
    └── scanner         # scanner adapters, later
```

## First implementation target

Start with one end-to-end deterministic source:

1. Fetch recent SEC company filing metadata.
2. Filter for filing types that can plausibly produce trade candidates.
3. Normalize each result into a candidate.
4. Attach source metadata and explanation text.
5. Hand candidates to the application layer for enrichment.

## Testing expectations

Tests should cover:

- parser behavior with representative source payloads
- malformed or missing fields
- duplicate candidate handling
- source adapter failures
- deterministic mapping from raw payload to normalized candidate

Use fixture files for realistic source samples instead of building every test payload inline.
