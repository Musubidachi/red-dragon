# SEC EDGAR Ingestion

Index for the SEC ingestion design docs.

| Doc | What's in it |
| --- | --- |
| [SEC_API.md](./SEC_API.md) | SEC API rules, headers, rate limits, hosts, endpoints, and URL gotchas. |
| [SEC_FORMS.md](./SEC_FORMS.md) | Forms in scope, 8-K item taxonomy, Form 4 XML notes, and 13D/G parsing notes. |
| [SEC_IMPLEMENTATION.md](./SEC_IMPLEMENTATION.md) | Original implementation plan, future polling design, and remaining open decisions. |

## Current Implementation

The current code exposes `SecIngestionService`, which fetches a company's SEC
submissions by CIK and emits normalized `TradeCandidate` objects.

Implemented today:

* Submissions endpoint client.
* Required SEC User-Agent and simple global rate limiter.
* Flattening of the SEC column-oriented recent-filings payload.
* In-scope form filtering: `8-K`, `4`, `SC 13D`, `SC 13G`, `S-1`, `S-3`, `424B`.
* 8-K item-code mapping to internal catalyst categories.
* Transparent ingest-time scores for structural reality, materiality, earlyness,
  and reflexivity.
* Spring wiring in `app/config/PipelineConfiguration.java`.

Not implemented yet:

* Generic `Source` SPI / `Candidate` interface.
* Structured `SecCandidate` and per-form payload hierarchy.
* CIK-to-ticker map loading from `company_tickers*.json`.
* Latest-filings RSS firehose polling.
* Persistent per-CIK cursor state or SEC dedupe state outside candidate id checks.
* Full Form 4 XML, 13D/G body, offering body, or XBRL parsing.

## Boundary

SEC ingestion does not persist, place orders, inspect a portfolio, or decide
whether a candidate is tradable. It emits `TradeCandidate`; `app` then enriches,
validates, and persists the pipeline result.
