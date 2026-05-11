# sec — SEC EDGAR Ingestion Adapter

Pulls trade candidates from the SEC's EDGAR submissions feed.

## Reading order for a new developer

Every class in this package follows the same convention:

- **One job per class.** The class name describes what it does.
- **One public `process()` method** is the entry point.
- **Each helper method has one responsibility.** When a method does two things, it is split.

Read the classes in this order to understand the flow end-to-end:

| # | Class                                                  | What its `process()` does                                          |
| - | ------------------------------------------------------ | ------------------------------------------------------------------ |
| 1 | [`SecIngestionService`](./SecIngestionService.java)    | Given a CIK, returns a list of `TradeCandidate`s. Orchestrator.    |
| 2 | [`SubmissionsClient`](./SubmissionsClient.java)        | Fetches the submissions JSON for a CIK and deserialises it.        |
| 3 | [`SecHttpClient`](./SecHttpClient.java)                | Performs a single rate-limited HTTP GET to sec.gov.                |
| 4 | [`SimpleRateLimiter`](./SimpleRateLimiter.java)        | Blocks until a token is available; tokens refill every second.    |
| 5 | [`SubmissionsFilingExtractor`](./SubmissionsFilingExtractor.java) | Flattens the SEC's column-oriented payload into `SecFiling`s. |
| 6 | [`EightKCategoryMapper`](./EightKCategoryMapper.java)  | Maps a list of 8-K Item codes to one `CandidateCatalystType`.      |
| 7 | [`SecCandidateBuilder`](./SecCandidateBuilder.java)    | Builds a fully scored `TradeCandidate` from a `SecFiling`.        |

## Data shapes

| Shape                                              | Role                                                                 |
| -------------------------------------------------- | -------------------------------------------------------------------- |
| [`SubmissionsResponse`](./SubmissionsResponse.java) | Raw Jackson view of the SEC submissions JSON. Column-oriented.       |
| [`SecFiling`](./SecFiling.java)                    | One filing, denormalised. The unit of work for the rest of the flow. |
| `TradeCandidate` (parent package)                  | What this adapter ultimately emits.                                  |
| [`SecApiProperties`](./SecApiProperties.java)      | User-Agent, base URL, rate limit. Inject one instance.               |

## Wiring

This adapter is not yet wired into Spring beans. To use it from `app`,
construct the chain once at startup:

```java
SecApiProperties props      = SecApiProperties.defaults();  // override User-Agent for prod
SimpleRateLimiter limiter   = new SimpleRateLimiter(props.getRequestsPerSecond());
SecHttpClient http          = new SecHttpClient(props, limiter);
SubmissionsClient subs      = new SubmissionsClient(props, http);
SubmissionsFilingExtractor extractor = new SubmissionsFilingExtractor();
EightKCategoryMapper mapper = new EightKCategoryMapper();
SecCandidateBuilder builder = new SecCandidateBuilder(mapper);
SecIngestionService service = new SecIngestionService(subs, extractor, builder);

List<TradeCandidate> candidates = service.process("789019");   // Microsoft
```

## Scope

In scope today:

- 8-K, Form 4, 13D/G, S-1, S-3, 424B (filtered in `SubmissionsFilingExtractor`).
- Deterministic scoring at ingest. Transparent, no ML.

Out of scope (deferred):

- Form 4 / 13D body parsing (we read the index and link to the document).
- XBRL parsing of financial statements.
- News, scanners, macro feeds.
- Persistence and de-duplication across runs (caller's responsibility).

See `../../../../../../../SEC_API.md`, `SEC_FORMS.md`, and `SEC_IMPLEMENTATION.md`
in the lib-ingestion root for the design rationale behind these choices.
