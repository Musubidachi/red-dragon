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
| 6 | [`CikLookupService`](./CikLookupService.java) | Resolves ticker to padded CIK with TTL refresh and fail-stale cache behavior. |
| 7 | [`SubmissionsFilingExtractor`](./SubmissionsFilingExtractor.java) | Flattens the SEC column-oriented payload into `SecFiling`s. |
| 8 | [`EightKCategoryMapper`](./EightKCategoryMapper.java) | Maps a list of 8-K item codes to one `CandidateCatalystType`. |
| 9 | [`SecFilingScoringHeuristics`](./SecFilingScoringHeuristics.java) | Scores SEC filing metadata before candidate construction. |
| 10 | [`SecCandidateBuilder`](./SecCandidateBuilder.java) | Builds a fully scored `TradeCandidate` from a `SecFiling`. |

## Data shapes

| Shape | Role |
| --- | --- |
| [`SubmissionsResponse`](../../models/sec/SubmissionsResponse.java) | Raw Jackson view of the SEC submissions JSON. Column-oriented. |
| [`SecFiling`](../../models/sec/SecFiling.java) | One filing, denormalized. The unit of work for the rest of the flow. |
| `TradeCandidate` | What this adapter ultimately emits. The shared type lives in `lib-domain`. |
| [`SecApiProperties`](../../config/SecApiProperties.java) | User-Agent, base URL, rate limit, timeout, retry, company-tickers URL, and ticker-cache TTL settings. |

## Wiring

This adapter is wired into Spring by `app/config/PipelineConfiguration.java`.
The effective construction is:

```java
SecApiProperties props = ...; // from red-dragon.sec.* properties
SimpleRateLimiter limiter = new SimpleRateLimiter(props.getRequestsPerSecond());
SecHttpClient http = new SecHttpClient(props, limiter);
SubmissionsClient subs = new SubmissionsClient(props, http);
SubmissionsFilingExtractor extractor = new SubmissionsFilingExtractor();
EightKCategoryMapper mapper = new EightKCategoryMapper();
SecFilingScoringHeuristics scoring = new SecFilingScoringHeuristics();
SecCandidateBuilder builder = new SecCandidateBuilder(mapper, scoring);
CikLookupService cikLookup = new CikLookupService(props, http);
SecIngestionService service = new SecIngestionService(subs, extractor, builder, cikLookup);

List<TradeCandidate> candidates = service.process("789019"); // Microsoft
List<TradeCandidate> byTicker = service.processByTicker("MSFT");
```

## Scope

In scope today:

* 8-K, Form 4, 13D/G, S-1, S-3, and 424B forms filtered in
  `SubmissionsFilingExtractor`.
* Deterministic scoring at ingest. Transparent, no ML.
* Submissions-endpoint polling by CIK or ticker.
* TTL-managed `company_tickers*.json` ticker-to-CIK lookup with fail-stale
  refresh after the first successful load.

Out of scope or deferred:

* Form 4 / 13D body parsing. Current code reads index metadata and links to the
  primary document.
* Latest-filings RSS firehose polling.
* CIK-to-tickers reverse map for future CIK-first firehose ingestion.
* XBRL parsing of financial statements.
* News, scanners, macro feeds, and LLM ticker scheduler.
* Persistence and deduplication across runs. That remains the caller's
  responsibility.

See [`SEC_API.md`](../../../../../../../../SEC_API.md),
[`SEC_FORMS.md`](../../../../../../../../SEC_FORMS.md), and
[`SEC_IMPLEMENTATION.md`](../../../../../../../../SEC_IMPLEMENTATION.md) in the
lib-ingestion root for the design rationale behind these choices.
