# sec - SEC EDGAR Ingestion Adapter

Pulls trade candidates from the SEC EDGAR submissions feed.

## Reading order for a new developer

Every class in this package follows the same convention:

* One job per class. The class name describes what it does.
* One public `process()` method is the entry point.
* Each helper method has one responsibility. When a method does two things, it
  is split.

Read the classes in this order to understand the flow end to end:

| # | Class | What its `process()` does |
| - | --- | --- |
| 1 | [`SecIngestionService`](./SecIngestionService.java) | Given a CIK, returns a list of `TradeCandidate`s. Orchestrator. |
| 2 | [`SubmissionsClient`](./SubmissionsClient.java) | Fetches the submissions JSON for a CIK and deserializes it. |
| 3 | [`SecHttpClient`](./SecHttpClient.java) | Performs a single rate-limited HTTP GET to sec.gov. |
| 4 | [`SimpleRateLimiter`](./SimpleRateLimiter.java) | Blocks until a token is available; tokens refill every second. |
| 5 | [`CikFormats`](./CikFormats.java) | Normalizes CIKs, accession numbers, and archive URL path segments. |
| 6 | [`CikLookupService`](./CikLookupService.java) | Resolves ticker to padded CIK and padded CIK to ordered tickers with TTL refresh and fail-stale cache behavior. |
| 7 | [`SubmissionsFilingExtractor`](./SubmissionsFilingExtractor.java) | Flattens the SEC column-oriented payload into `SecFiling`s. |
| 8 | [`SecFilingBodyClient`](./SecFilingBodyClient.java) | Fetches a filing primary-document body through `SecHttpClient` for explicit callers. |
| 9 | [`Form4OwnershipXmlParser`](./Form4OwnershipXmlParser.java) | Parses Form 4 ownership XML into issuer, owner, and non-derivative transaction records. |
| 10 | [`SecForm4BodySignalService`](./SecForm4BodySignalService.java) | Optionally fetches/parses a Form 4 body and emits one body-derived `TradeCandidate`. |
| 11 | [`EightKCategoryMapper`](./EightKCategoryMapper.java) | Maps a list of 8-K item codes to one `CandidateCatalystType`. |
| 12 | [`SecFilingScoringHeuristics`](./SecFilingScoringHeuristics.java) | Scores SEC filing metadata before candidate construction. |
| 13 | [`SecCandidateBuilder`](./SecCandidateBuilder.java) | Builds a fully scored metadata-level `TradeCandidate` from a `SecFiling`. |

## Data shapes

| Shape | Role |
| --- | --- |
| [`SubmissionsResponse`](../../models/sec/SubmissionsResponse.java) | Raw Jackson view of the SEC submissions JSON. Column-oriented. |
| [`SecFiling`](../../models/sec/SecFiling.java) | One filing, denormalized. The unit of work for the rest of the flow. |
| [`Form4OwnershipReport`](../../models/sec/Form4OwnershipReport.java) | Parsed Form 4 ownership body fields for explicit body-parser callers. |
| `TradeCandidate` | What this adapter ultimately emits. The shared type lives in `lib-domain`. |
| [`SecApiProperties`](../../config/SecApiProperties.java) | User-Agent, base URL, rate limit, timeout, retry, company-tickers URL, and ticker-cache TTL settings. |

## Wiring

This adapter is wired into Spring by `app/config/PipelineConfiguration.java`.
The effective construction is:

```java
SecApiProperties props = ...; // from red-dragon.sec.* properties
ObjectMapper objectMapper = ...; // shared Spring-managed mapper
SimpleRateLimiter limiter = new SimpleRateLimiter(props.getRequestsPerSecond());
SecHttpClient http = new SecHttpClient(props, limiter);
SubmissionsClient subs = new SubmissionsClient(props, http, objectMapper);
SubmissionsFilingExtractor extractor = new SubmissionsFilingExtractor();
EightKCategoryMapper mapper = new EightKCategoryMapper();
SecFilingScoringHeuristics scoring = new SecFilingScoringHeuristics();
SecCandidateBuilder builder = new SecCandidateBuilder(mapper, scoring);
CikLookupService cikLookup = new CikLookupService(props, http, objectMapper);
SecIngestionService service = new SecIngestionService(subs, extractor, builder, cikLookup);

List<TradeCandidate> candidates = service.process("789019"); // Microsoft
List<TradeCandidate> byTicker = service.processByTicker("MSFT");
List<String> tickers = cikLookup.tickersForCik("789019");
```

The RD-M2 body-parser slice is intentionally separate from the app wiring:

```java
SecFilingBodyClient bodyClient = new SecFilingBodyClient(http);
Form4OwnershipXmlParser form4Parser = new Form4OwnershipXmlParser();
SecForm4BodySignalService bodySignalService = new SecForm4BodySignalService(bodyClient, form4Parser);

String body = bodyClient.process(filing);
Form4OwnershipReport ownership = form4Parser.process(body);
Optional<TradeCandidate> bodyCandidate = bodySignalService.process(filing);
```

## Scope

In scope today:

* 8-K, Form 4, 13D/G, S-1, S-3, and 424B forms filtered in
  `SubmissionsFilingExtractor`.
* Deterministic scoring at ingest. Transparent, no ML.
* Submissions-endpoint polling by CIK or ticker.
* TTL-managed `company_tickers*.json` ticker-to-CIK lookup with fail-stale
  refresh after the first successful load.
* CIK-to-tickers reverse lookup for future CIK-first firehose ingestion.
  Multi-class issuers emit all SEC-listed tickers in source order; the
  preferred ticker is the first SEC-listed symbol.
* Explicit primary-document fetches and Form 4 XML parsing for ownership
  reports.
* Explicit Form 4 body-derived `TradeCandidate` creation through
  `SecForm4BodySignalService`. Non-Form-4 filings return empty without a body
  fetch. The default `SecIngestionService` remains metadata-driven.

Out of scope or deferred:

* Form 4 derivative tables, holdings-only rows, footnotes, and final signal
  semantics.
* 13D/G body parsing.
* Latest-filings RSS firehose polling.
* XBRL parsing of financial statements.
* News, scanners, macro feeds, and LLM ticker scheduler.
* Persistence and deduplication across runs. That remains the caller's
  responsibility.

See [`SEC_API.md`](../../../../../../../../SEC_API.md),
[`SEC_FORMS.md`](../../../../../../../../SEC_FORMS.md), and
[`SEC_IMPLEMENTATION.md`](../../../../../../../../SEC_IMPLEMENTATION.md) in the
lib-ingestion root for the design rationale behind these choices.
