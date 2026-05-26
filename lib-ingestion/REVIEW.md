# lib-ingestion — Implementation Review (2026-05-17)

**Last fix pass:** 2026-05-20 (low-hanging) + 2026-05-21 (SEC HTTP hardening — timeouts, retry/backoff with jitter, token-bucket rebuild) + 2026-05-21 (8-K taxonomy extension — M&A label, disclosure routing, expanded SEVERE / DILUTION buckets) + 2026-05-21 (ticker handling — OTC filtering via SubmissionsResponse.exchanges, multi-class disambiguation, drop tickerless filings) + 2026-05-24 (URL/CIK normalisation — new `CikFormats` utility owns padding / stripping / accession-dash / URL-encoding, `SubmissionsClient` and `CikLookupService` delegate to it, `SecFiling.primaryDocumentUrl()` is defensive and URL-encodes filenames, `companyTickersUrl` is now a `SecApiProperties` field) + 2026-05-25 (form-variant matching — `/A` amendments + `424B1..424B5` accepted, surgical so `40-F` / `S-11` stay out of scope) + 2026-05-25 (acceptanceDateTime parsing closes the UTC-midnight earlyness tier-jump; isXBRL/isInlineXBRL columns also pulled into `SubmissionsRecentFilings`).

Audit of `lib-ingestion` against its in-module design docs (`README.md`,
`SEC_API.md`, `SEC_FORMS.md`, `SEC_INGESTION.md`, `SEC_IMPLEMENTATION.md`,
`LLM_TICKER_SCHEDULER.md`, `services/sec/README.md`) and the parent docs
(`../README.md`, `../ARCHITECTURE.md`).

---

## Fix Status

A subset of the findings below have been addressed in the 2026-05-20 local-fix pass. The full report is preserved unchanged after this header for traceability.

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | High | M&A items map to `CONTRACT` (taxonomy collapse) | **Fixed** — new `MERGER_AND_ACQUISITION` value added to `CandidateCatalystType` (lib-domain) between CONTRACT and SUPPLY_CONSTRAINT; `EightKCategoryMapper.process` routes the MNA bucket (2.01, 5.01) there; M&A wins over CONTRACT when both present; `SecCandidateBuilder.scoreStructuralReality` scores M&A at 0.92 (just above CONTRACT) since it's a binary corporate event. |
| 2 | High | Items 7.01 / 8.01 fall through to `FILING_EVENT` | **Fixed** — new `DISCLOSURE_ITEMS = Set.of("7.01", "8.01")` bucket routes Reg FD + Other Events to the existing `NEWS_EVENT` catalyst. `SecFilingScoringHeuristics` adds a DISCLOSURE bucket with materiality 0.55 (depends on attachment) and reflexivity 0.60 (deliberate disclosure usually gets press). `SecCandidateBuilder.scoreStructuralReality` scores NEWS_EVENT at 0.65 (the real signal lives in the attached release, not the form itself). |
| 3 | High | Items 2.03 / 2.05 / 2.06 unhandled | **Fixed** — 2.03 (creation of direct financial obligation) added to `DILUTION_ITEMS`; 2.05 (exit/disposal of business) and 2.06 (material impairments) added to `SEVERE_ITEMS`. `SecFilingScoringHeuristics` buckets are kept in lockstep with `EightKCategoryMapper`. |
| 4 | High | No HTTP retry / exponential backoff on 429/503 | **Fixed** — `SecHttpClient.fetchWithRetry` retries on 429 + 5xx + I/O errors with jittered exponential backoff (base × 2^attempt, 0–25% jitter, capped at `maxBackoffMillis`); 4xx non-429 propagates immediately; each retry re-acquires a rate-limit token |
| 5 | High | No HTTP timeouts on SEC client | **Fixed** — `SecApiProperties` now carries `connectTimeoutMillis` and `readTimeoutMillis` (5s / 30s defaults, validated bounds). `SecHttpClient` builds the default `RestClient` with a `JdkClientHttpRequestFactory` configured with the read timeout |
| 6 | High | Default User-Agent is `replace-me@example.invalid` | **Fixed** — `SecApiProperties` constructor now rejects placeholder UAs (replace-me, example.invalid, example.com, noreply@, etc.) and enforces `requestsPerSecond ∈ [1, 10]`; `defaults()` removed |
| 7 | High | `SimpleRateLimiter` bursty refill, not a true token bucket | **Fixed** — `ScheduledExecutorService.scheduleAtFixedRate` releases one permit every `1000/capacity` ms; refill clamped at capacity so an idle bucket cannot accumulate beyond its ceiling |
| 8 | Medium | `SimpleRateLimiter` has no timeout / `tryAcquire` | **Fixed** — added `process(Duration timeout)` (with `DEFAULT_ACQUIRE_TIMEOUT = 60s`) and `tryAcquire()`; default `process()` now bounded |
| 9 | Medium | Refill thread isn't a `ScheduledExecutorService` | **Fixed** — replaced raw daemon thread with `Executors.newSingleThreadScheduledExecutor(daemonFactory)`; class implements `AutoCloseable`; `shutdown()` / `close()` exposed |
| 10 | Medium | Hard-coded base URL; only `data.sec.gov/submissions` configurable | **Fixed** — `SecApiProperties` now exposes `companyTickersUrl` (defaults to `https://www.sec.gov/files/company_tickers.json` via `DEFAULT_COMPANY_TICKERS_URL`); `CikLookupService` reads it from properties instead of the hardcoded URI it used to embed. `PipelineConfiguration.secApiProperties` accepts `${red-dragon.sec.company-tickers-url}` (defaulting to the live URL) and all other knobs as overrideable `@Value` bindings, so an integration test can point at a stub host without touching code. |
| 11 | Medium | `CikLookupService` no TTL / refresh | Open |
| 12 | Medium | CIK return is non-padded | **Fixed** — `CikLookupService.process(ticker)` now returns the 10-digit zero-padded form (canonical submissions-endpoint shape). Normalisation is done once at load time via `CikFormats.padCik(cik_str)`; downstream callers no longer need to remember to re-pad. The `Optional<String>` contract is unchanged, so `TickerAnalysisService` and other consumers see no API drift. |
| 13 | Medium | Share-class multiplicity collapsed (in `CikLookupService`) | Open — connected to #20 (CikLookupService is still dead code). Note that share-class disambiguation IS now done in `SubmissionsFilingExtractor` (see #14 fix) for the live ingestion path; this finding tracks the parallel concern for the lookup-by-ticker future work. |
| 14 | Medium | `SubmissionsFilingExtractor` picks `tickers[0]` blindly | **Fixed** — `SubmissionsResponse` extended with an `exchanges` parallel list (backwards-compatible 4-arg constructor preserves existing test callers). New `SubmissionsFilingExtractor.selectTicker` pairs `tickers[i]` with `exchanges[i]`, filters out tickers whose exchange contains "OTC" / "Pink", and emits the first remaining ticker (SEC's primary-class-first convention preserved). Tickers with missing/blank exchange data are kept defensively. Multi-class collapses logged at DEBUG; tickerless drops logged at WARN. |
| 15 | Medium | `SubmissionsClient.zeroPadCik` brittle parsing | **Fixed** — local `zeroPadCik` method deleted; `SubmissionsClient.buildSubmissionsUri` now delegates to `CikFormats.padCik(cik)` which accepts bare digits, already-padded form, and `CIK0000…` prefix (case-insensitive), and throws a descriptive `IllegalArgumentException` (not `NumberFormatException`) on non-digit or overlong input. Idempotent on already-padded inputs. |
| 16 | Medium | `SecFiling.primaryDocumentUrl` undefensive | **Fixed** — returns `null` instead of throwing when CIK or accession are missing/malformed; CIK normalisation goes through `CikFormats.stripLeadingZeros`, accession through `CikFormats.accessionNoDashes`, and the primary-document filename is URL-encoded via `CikFormats.encodePathSegment` (spaces → `%20`, accented characters escaped). `TradeCandidate.sourceUrl` already runs through `IngestionTextUtils.clean`, which tolerates the `null` return cleanly. |
| 17 | Medium | `SecCandidateBuilder` substitutes `"UNKNOWN"` ticker | **Fixed** — `tickerOrPlaceholder` removed. `SubmissionsFilingExtractor` now drops filings whose issuer has no usable ticker at source (logs WARN), so a tickerless filing never reaches the candidate builder. `SecCandidateBuilder.process` adds a defensive `Objects.requireNonNull(filing)` + explicit non-blank ticker check that throws `IllegalArgumentException` if the contract is violated. |
| 18 | Medium | Earlyness anchored to UTC-midnight produces tier-jumps at midnight | **Fixed** — `SubmissionsRecentFilings` extended with parallel `acceptanceDateTime`, `isXBRL`, `isInlineXBRL` columns (Finding #19 piggybacks). `SubmissionsFilingExtractor.acceptanceDateTime` parses the SEC's Eastern-time `YYYY-MM-DDTHH:MM:SS` format via `LocalDateTime.parse(...).atZone(America/New_York).toInstant()`. `SecFiling.acceptanceDateTime` carries the value through; `SecCandidateBuilder.filingObservedAt` prefers it when present and falls back to `filingDate.atStartOfDay(UTC)` (legacy behaviour) when absent. Backwards-compatible 9-arg `SecFiling` and 6-arg `SubmissionsRecentFilings` constructors preserved so older tests keep compiling. Three new tests pin the Eastern→UTC conversion, the fallback path, and malformed-timestamp resilience (logs a warn, keeps the row). |
| 19 | Medium | `SubmissionsResponse` missing `isXBRL`/`isInlineXBRL`/`acceptanceDateTime` | **Fixed** — all three columns added to `SubmissionsRecentFilings` (alongside the existing six). `acceptanceDateTime` is consumed by Finding #18; `isXBRL` / `isInlineXBRL` are parsed and held but not yet routed downstream — future XBRL-aware filtering is a one-line extractor hop. Backwards-compatible 6-arg constructor preserves all existing test fixtures. |
| 20 | Medium | `CikLookupService` is dead code | Open |
| 21 | Medium | `ManualCandidateIngestionService` no input validation | **Fixed** — explicit `ValidationScoreUtils.requireNormalized` on all four scores; blank-symbol rejected ahead of the constructor |
| 22 | Low | Manual `candidateId` is random UUID, defeats idempotency | **Fixed** — derived from `SHA-256("MANUAL\|symbol\|date\|headline")` so repeat-submits collide and the orchestrator dedup catches them |
| 23 | Low | `ManualCandidateIngestionService` no `Clock` injection | **Fixed** — accepts a `Clock` in a new constructor; default constructor delegates to `Clock.systemUTC()` |
| 24 | Low | `FORMS_IN_SCOPE` misses `/A` and `424B[1-5]` variants | **Fixed** — `SubmissionsFilingExtractor.isInScope` now accepts the canonical forms, their `/A` amendments (`8-K/A`, `4/A`, `SC 13D/A`, `SC 13G/A`, `S-1/A`, `S-3/A`), and the numbered prospectus series via a dedicated `FOUR_TWO_FOUR_B_SUBFORMS = {424B1..424B5}` set. Matching is surgical (not naive prefix) so unrelated forms that happen to share a prefix — `40-F` (foreign issuer annual report), `S-11` (real-estate registration) — correctly stay out of scope. Three new tests cover amendments, the 424B series, and the prefix-collision negatives. |
| 25 | Low | 8-K item `9.01` standalone treated as real category | **Fixed** — `EightKCategoryMapper.process` now strips `9.01` before category classification; falls back to `FILING_EVENT` if `9.01` was the only item |
| 26 | Low | `EightKCategoryMapper` priority order not documented | **Fixed** — added priority-and-rationale block to the `process(...)` javadoc enumerating SEVERE → M&A → CONTRACT → DILUTION → EARNINGS → GOVERNANCE and why each tier wins |
| 27 | Low | Unreachable `default` branch in `scoreStructuralReality` | **Fixed** — replaced with `default -> throw new IllegalStateException(...)` so future contributors who extend `pickCatalystType` are forced to update the mapping |

**Fixed across all passes: 24** (#1, #2, #3, #4, #5, #6, #7, #8, #9, #10, #12, #14, #15, #16, #17, #18, #19, #21, #22, #23, #24, #25, #26, #27). **Open: 3** — CikLookupService still single-loaded with no TTL (#11), CIK→tickers reverse map for future Loop B (#13), CikLookupService not yet plumbed end-to-end (#20).

---

---

## Expected Behavior (per docs)

### `README.md` (module root)

- Module turns outside information into normalized `TradeCandidate` objects
  (L1 + the candidate half of L2).
- Implemented sources today: `ManualCandidateIngestionService` and
  `services/sec/SecIngestionService`.
- SEC adapter: fetches recent company submissions by CIK, filters in-scope
  forms, flattens metadata, maps 8-K item codes, builds scored candidates.
- Non-responsibilities: does not classify regime, fetch OHLCV bars, persist,
  place orders, or know portfolio state.
- Planned / deferred: generic source SPI, news/RSS, scanner, macro adapters,
  SEC RSS firehose, CIK/ticker cache, Form 4 / 13D-G / offering body / XBRL
  parsing.

### `SEC_API.md`

- `User-Agent` MUST identify requester and a real contact email; configurable
  via env/Spring property; fail-fast at startup if missing.
- Reject defaults like `example.com`, `noreply@`, generic UAs.
- Rate limit: ≤ 10 req/sec across all SEC hosts; token-bucket 10 capacity,
  refill 10/sec; start at 5/sec to leave retry headroom.
- `Accept-Encoding: gzip, deflate`; HTTPS only.
- Hosts: `www.sec.gov`, `data.sec.gov`, `efts.sec.gov`.
- Backoff on 429/503: exponential, jittered, max 5 retries, max delay 30s.
- CIK URL forms: 10-digit zero-padded for submissions, leading zeros stripped
  for archives. Accession-number forms: with/without dashes.
- Endpoints to wire: `company_tickers.json`, submissions API, primary doc URL,
  EFTS (deferred), latest-filings RSS (deferred), daily index (deferred).

### `SEC_FORMS.md`

- In-scope forms (priority order): 8-K, Form 4, 13D, 13G, S-1, S-3, 424B,
  Form 3, Form 5 (last two recommended to *drop initially*).
- Authoritative 8-K item taxonomy with 21 item codes, mapped to seven
  catalyst categories: `earnings`, `M&A`, `contract`, `governance`,
  `severe-negative`, `dilution`, `disclosure`, `other`.
- Specifically called out: 7.01, 8.01 (disclosure); 9.01 alone meaningless;
  ties broken to highest-signal category with auditable rules.
- Form 4 XML extraction spec (issuer/owner/transaction/10b5-1 flag).
- 13D/G HTML cover-page extraction; activist vs passive flag.

### `SEC_INGESTION.md` / `SEC_IMPLEMENTATION.md`

- Suggested implementation order: SPI → rate limiter + UA + gzip + **backoff**
  → CIK-ticker map (weekly refresh, fail-stale) → submissions client → Loop A
  (per-CIK polling) → 8-K → Form 4 → 13D/G → Loop B (Atom firehose) → offerings.
- Rate limiter: hand-rolled Semaphore + ScheduledExecutorService refilling 10
  permits every second; wrap every request including retries.
- Per-CIK cursor + global dedup state; in-memory CIK-ticker cache with
  fail-stale policy on refresh failure.
- Status note (SEC_INGESTION.md §1): submissions endpoint client, UA + global
  rate limiter, column-payload flattening, 8-K item mapping, ingest-time
  transparent scores, Spring wiring already done.

### `LLM_TICKER_SCHEDULER.md`

- `services/llm/` package with `LlmTickerScheduler`, `LlmTickerIngestionService`,
  `LlmTickerAggregator`, `TickerPromptBuilder`, `TickerSuggestionParser`,
  provider adapters (`ClaudeLlmProvider`, `OpenAiLlmProvider`, `GeminiLlmProvider`,
  `MockLlmProvider`).
- `models/llm/` package with `LlmTickerSuggestion`, `LlmResponseEnvelope`,
  `LlmAggregatedSuggestion`.
- New `SourceType.LLM_SUGGESTION` enum value.
- New `red-dragon.scheduler.llm.*` and `red-dragon.llm.providers.*` config.
- Flyway `V4__llm_audit.sql` table `llm_run`.

### `services/sec/README.md`

- One job per class, one public `process()` per class. Reading order:
  `SecIngestionService` → `SubmissionsClient` → `SecHttpClient` →
  `SimpleRateLimiter` → `SubmissionsFilingExtractor` → `EightKCategoryMapper`
  → `SecCandidateBuilder`. Notes `CikLookupService` not yet wired.

---

## Files Reviewed

### SEC HTTP / rate limiting
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\config\SecApiProperties.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\SimpleRateLimiter.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\SecHttpClient.java`

### Submissions ingestion pipeline
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\SecIngestionService.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\SubmissionsClient.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\SubmissionsFilingExtractor.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\models\sec\SubmissionsResponse.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\models\sec\SubmissionsFilings.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\models\sec\SubmissionsRecentFilings.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\models\sec\SecFiling.java`

### 8-K mapping / scoring
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\EightKCategoryMapper.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\SecFilingScoringHeuristics.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\SecCandidateBuilder.java`

### CIK lookup / scheduling
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\sec\CikLookupService.java`
- (no scheduler — see Doc/Code Drift)

### Manual ingestion
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\main\java\dev\reddragon\ingestion\services\ManualCandidateIngestionService.java`

### Tests
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\test\java\dev\reddragon\ingestion\services\sec\SimpleRateLimiterTest.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\test\java\dev\reddragon\ingestion\services\sec\EightKCategoryMapperTest.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\test\java\dev\reddragon\ingestion\services\sec\SecCandidateBuilderTest.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\test\java\dev\reddragon\ingestion\services\sec\SecIngestionServiceTest.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\test\java\dev\reddragon\ingestion\services\sec\SubmissionsFilingExtractorTest.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\test\java\dev\reddragon\ingestion\services\sec\SecFilingScoringHeuristicsTest.java`
- `C:\repos\red-dragon\red-dragon\lib-ingestion\src\test\java\dev\reddragon\ingestion\services\ManualCandidateIngestionServiceTest.java`

---

## Findings

### 1. **Severity: High** — `EightKCategoryMapper` maps M&A items to `CONTRACT`, contradicting taxonomy and overriding contract items
- **File:** `services/sec/EightKCategoryMapper.java:38-39`
- **Expected (SEC_FORMS.md §2):** Items `2.01` and `5.01` belong to the **`M&A`** category; the doc explicitly says "M&A takes priority" over contract.
- **Actual:**
  ```java
  if (containsAny(itemCodes, MNA_ITEMS))        return CandidateCatalystType.CONTRACT;
  if (containsAny(itemCodes, CONTRACT_ITEMS))   return CandidateCatalystType.CONTRACT;
  ```
  Both M&A and contract items resolve to `CONTRACT`. Because the domain enum has no `M&A`/`MERGER_AND_ACQUISITION` value, the mapper collapses two distinct catalyst categories into one label. The test `mna_takes_priority_over_contract` even asserts `CONTRACT` for `2.01` + `1.01`, locking in the loss of fidelity.
- **Why it matters:** The whole point of the 8-K taxonomy in `SEC_FORMS.md` is to give downstream a structured signal. Treating an asset acquisition (`2.01`) the same as a routine material agreement (`1.01`) destroys signal that `lib-analytics` / `lib-validation` could otherwise use. It also defeats `SEC_FORMS.md`'s "Suggested first-pass classification" with seven category labels.
- **Proposed fix:** Either (a) extend `CandidateCatalystType` with `MERGER_AND_ACQUISITION` and route `MNA_ITEMS` to it, or (b) document explicitly in `services/sec/README.md` and `SEC_FORMS.md` that M&A is coerced to `CONTRACT` and explain why.

### 2. **Severity: High** — 8-K items `7.01` (Reg FD) and `8.01` (Other Events) silently fall through, contradicting `SEC_FORMS.md`
- **File:** `services/sec/EightKCategoryMapper.java:19-44` and `SecFilingScoringHeuristics.java:36-93`
- **Expected (SEC_FORMS.md §2 + §"Notes"):** Items 7.01 (Reg FD Disclosure) and 8.01 (Other Events) map to the **`disclosure` / `press`** catalyst category; their value is whether the attached press release is material — the doc explicitly calls these out as worth surfacing.
- **Actual:** Neither item appears in any of `SEVERE_ITEMS`, `MNA_ITEMS`, `CONTRACT_ITEMS`, `DILUTION_ITEMS`, `EARNINGS_ITEMS`, or `GOVERNANCE_ITEMS`. An 8-K with only `7.01,9.01` (the most common press-release shape) falls through to `FILING_EVENT` with materiality `0.40` and reflexivity `0.30`.
- **Why it matters:** A large fraction of 8-Ks are exactly the Reg FD/press-release combo (FDA approvals, partnerships, government-contract announcements). Treating them as the lowest-signal residual hides exactly the catalysts the framework targets.
- **Proposed fix:** Add `DISCLOSURE_ITEMS = Set.of("7.01", "8.01")` and route it to a dedicated catalyst (e.g. `NEWS_EVENT`) with materiality ~0.55 and reflexivity ~0.60.

### 3. **Severity: High** — 8-K item `2.03` (debt obligations) and `2.05`, `2.06` (restructuring, impairments) are unhandled
- **File:** `services/sec/EightKCategoryMapper.java:19-44`, `SecFilingScoringHeuristics.java:36-41`
- **Expected (SEC_FORMS.md §2):** Items `2.03` (creation of direct financial obligation) → balance-sheet event, `2.05` (exit/disposal) → restructuring, `2.06` (material impairments) → negative.
- **Actual:** None of these codes appears in either the mapper or the scoring heuristics. A debt blow-out (2.03) or a $2 B impairment (2.06) currently scores materiality 0.40 / reflexivity 0.30 — same as an unrecognised 8-K.
- **Why it matters:** Material impairments are exactly the kind of disequilibrium event the framework cares about; the explicit doc enumerates them.
- **Proposed fix:** Add 2.03, 2.05, 2.06 (and consider adding 5.02 stricter handling). 2.06 belongs in `SEVERE_ITEMS`; 2.03 + 2.05 deserve their own bucket or join `DILUTION_ITEMS`/`SEVERE_ITEMS` per doc intent.

### 4. **Severity: High** — No HTTP retry / exponential backoff on `429` / `503`, contradicting `SEC_API.md` §1 "Backoff"
- **File:** `services/sec/SecHttpClient.java:52-60`
- **Expected (SEC_API.md §1):** "On 429 Too Many Requests and 503: exponential, jittered, max 5 retries, max delay 30s."
- **Actual:**
  ```java
  private String fetchBody(URI uri) {
      return restClient.get()
              .uri(uri)
              .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
              .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
              .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
              .retrieve()
              .body(String.class);
  }
  ```
  No retry / backoff. Any 429 or 503 bubbles up as `HttpClientErrorException` / `HttpServerErrorException`, kills the current ingestion run.
- **Why it matters:** `SEC_API.md` flagged this as a known gap, but the gap matters operationally — a single 429 storm during a poll will fail the whole orchestrator pass.
- **Proposed fix:** Add a small retry loop around `fetchBody` with jittered exponential delay (e.g. base 500 ms, factor 2, max 30 s, max 5 attempts) and re-acquire a rate-limiter token on each retry (the limiter must wrap the retry too — `SEC_IMPLEMENTATION.md` §4 emphasises this).

### 5. **Severity: High** — No HTTP timeouts configured; calls can hang indefinitely
- **File:** `services/sec/SecHttpClient.java:33` and `SecApiProperties.java`
- **Expected (SEC_API.md §1 / general operational sanity):** Bounded request lifetime; a stuck call must not block the pipeline.
- **Actual:** `SecHttpClient` builds the `RestClient` with `RestClient.create()` — that uses the JDK `HttpClient` default which has no connect / read timeout. Nothing in `SecApiProperties` exposes a timeout field.
- **Why it matters:** A wedged SEC connection holds the scheduled poll thread forever; subsequent CIKs back up behind it. `SEC_INGESTION.md` calls polling out as a recurring scheduled job — that pattern relies on calls completing.
- **Proposed fix:** Add `connectTimeout` / `readTimeout` properties on `SecApiProperties` and build the `RestClient` with a configured `ClientHttpRequestFactory` (e.g. `JdkClientHttpRequestFactory` with `Duration.ofSeconds(10)` connect / `Duration.ofSeconds(30)` read).

### 6. **Severity: High** — Default `User-Agent` is `replace-me@example.invalid`, which `SEC_API.md` explicitly forbids
- **File:** `config/SecApiProperties.java:47-52`
- **Expected (SEC_API.md §1 "User-Agent"):** "Do not use `example.com`, `noreply@`, or fake emails. … Do not ship without setting this — config-driven, fail-fast on startup if missing."
- **Actual:**
  ```java
  public static SecApiProperties defaults() {
      return new SecApiProperties(
              "red-dragon (replace-me@example.invalid)",
              "https://data.sec.gov/submissions",
              5
      );
  }
  ```
  Nothing fails fast if the operator never overrides this. The properties class is not a Spring `@ConfigurationProperties` (no annotation, no validation), so a `@Bean defaults()` wiring would ship the placeholder UA without any startup error.
- **Why it matters:** Shipping the bogus UA gets the IP banned and silently degrades all SEC ingestion. The doc is explicit about fail-fast.
- **Proposed fix:** Either (a) drop `defaults()` and make `SecApiProperties` a `@ConfigurationProperties("red-dragon.sec")` POJO with `@NotBlank` / startup validation that rejects `example.invalid`, `replace-me`, `noreply@`, etc., or (b) keep `defaults()` but throw at construction if the UA matches the placeholder pattern.

### 7. **Severity: High** — `SimpleRateLimiter` refill loop is bursty and not a true 10/sec token bucket
- **File:** `services/sec/SimpleRateLimiter.java:51-72`
- **Expected (SEC_API.md §1 + SEC_IMPLEMENTATION.md §4):** Token bucket capacity N, refill N/sec. The doc says "Refill 10 permits every second" — but a sane implementation refills *gradually* (one permit every 1/N seconds) so the actual rate is bounded across the second boundary.
- **Actual:**
  ```java
  private void refillForever() {
      while (!Thread.currentThread().isInterrupted()) {
          sleepOneSecond();
          replenishBucket();
      }
  }

  private void replenishBucket() {
      int available = tokens.availablePermits();
      int missing = capacity - available;
      if (missing > 0) {
          tokens.release(missing);
      }
  }
  ```
  Permits are restored as one big batch at the end of every second. With `capacity = 5` you can issue 5 requests at t=0.99, then 5 more at t=1.01 — 10 requests inside 20 ms, which spikes above the 10/sec ceiling depending on the surrounding system, and is non-uniform regardless. Doc §4 in `SEC_IMPLEMENTATION.md` explicitly recommends "Refill 10 permits every second" via `ScheduledExecutorService` — same shape, but operationally this needs to be 1 permit per 100 ms (Option A's behaviour, Bucket4j-style) to be a real bucket.
- **Why it matters:** SEC fair-access bans depend on instantaneous rate, not average. A bursty limiter can clip the rule even when the per-second average is fine. Also: `replenishBucket()` reads `availablePermits()` then `release(missing)` — there's a small race where a consumer takes a permit between the two calls, causing temporary over-release (one extra permit per refill, capped by the next read).
- **Proposed fix:** Use `ScheduledExecutorService.scheduleAtFixedRate` to release one permit every `1000/capacity` ms; clamp `availablePermits()` at `capacity` via a `synchronized` block or a `Bucket4j` token bucket (recommended in `SEC_IMPLEMENTATION.md` §4 as Option A).

### 8. **Severity: Medium** — `SimpleRateLimiter` has no timeout / no `tryAcquire`; a starved caller blocks indefinitely
- **File:** `services/sec/SimpleRateLimiter.java:36-43`
- **Expected:** `SEC_IMPLEMENTATION.md` §4 implies bounded waits (so retries can budget themselves).
- **Actual:** `tokens.acquire()` has no timeout. If the refill thread dies (e.g. someone tweaks it later to throw) or the consumer count permanently exceeds refill rate, callers block forever.
- **Why it matters:** Limiter failure is silent. Couples with finding #4 — without backoff and with unbounded waits, a 429 storm followed by a misbehaving limiter wedges the poller.
- **Proposed fix:** Expose `process(Duration timeout)` that uses `Semaphore.tryAcquire(long, TimeUnit)` and throws a clearly named exception on timeout. Default to e.g. 60 s.

### 9. **Severity: Medium** — No fixed thread / executor for the refill thread; not a `ScheduledExecutorService` as the design doc suggests
- **File:** `services/sec/SimpleRateLimiter.java:45-49`
- **Expected (SEC_IMPLEMENTATION.md §4 Option B):** "hand-rolled `Semaphore` + `ScheduledExecutorService`".
- **Actual:** A raw daemon `Thread` is constructed in the constructor with `new Thread(this::refillForever, ...)`. No way to stop or replace it; constructing two `SimpleRateLimiter` instances spawns two refill threads.
- **Why it matters:** Thread leaks if the limiter is rebuilt at runtime (e.g. tests, reload). Harder to test (`sleepOneSecond()` is hard-coded). Loses the operational hooks `ScheduledExecutorService` gives you.
- **Proposed fix:** Use `Executors.newSingleThreadScheduledExecutor(daemonFactory)` and `scheduleAtFixedRate`; expose a `close()` / `shutdown()` method.

### 10. **Severity: Medium** — Hard-coded base URL `data.sec.gov/submissions` only — no support for `www.sec.gov` or `efts.sec.gov` hosts the doc enumerates
- **File:** `config/SecApiProperties.java:47-52`, `services/sec/CikLookupService.java:17`
- **Expected (SEC_API.md §1 "Hosts"):** Three SEC hosts — `www.sec.gov`, `data.sec.gov`, `efts.sec.gov`. The CIK-ticker map lives on `www.sec.gov/files/company_tickers.json`.
- **Actual:** `SecApiProperties` exposes only `submissionsBaseUrl`. `CikLookupService` hard-codes the ticker-map URL:
  ```java
  private static final URI COMPANY_TICKERS_URI = URI.create("https://www.sec.gov/files/company_tickers.json");
  ```
  Two hosts, two configuration shapes. No way to point to a staging mirror / test fixture without code change.
- **Why it matters:** Makes integration testing harder (can't stub URLs via properties) and contradicts the "config-driven" rule in `SEC_API.md`.
- **Proposed fix:** Add `companyTickersUrl` (and optionally `efTsBaseUrl`) to `SecApiProperties`; inject into `CikLookupService`.

### 11. **Severity: Medium** — `CikLookupService` has no TTL / refresh; loads `company_tickers.json` once and caches forever
- **File:** `services/sec/CikLookupService.java:32-39`
- **Expected (SEC_API.md §2 + SEC_IMPLEMENTATION.md §6 open decision 2):** "Refresh weekly; cache in memory with TTL." Recommended pattern: weekly refresh + fail-stale (keep last map and log).
- **Actual:**
  ```java
  public synchronized Optional<String> process(String ticker) {
      if (ticker == null || ticker.isBlank()) { return Optional.empty(); }
      if (tickerToCik == null) {
          tickerToCik = loadTickerMap();
      }
      return Optional.ofNullable(tickerToCik.get(normalize(ticker)));
  }
  ```
  Map is loaded once. New IPOs / renames never make it into the cache without a JVM restart. Failure during first load throws `IllegalStateException`, killing the caller and leaving the field `null` — next call retries fresh, no fail-stale safety net.
- **Why it matters:** Doc explicitly recommends weekly refresh + fail-stale. As written, ticker resolution silently rots over time.
- **Proposed fix:** Add `Instant lastRefreshAt` and refresh on access if older than configured TTL; on refresh failure, log + keep the previous map (fail-stale).

### 12. **Severity: Medium** — `CikLookupService` returns CIK without zero-padding; consumer must remember to pad
- **File:** `services/sec/CikLookupService.java:50-54`
- **Expected (SEC_API.md §3):** Submissions endpoint expects 10-digit zero-padded CIK. Doc says "Build a small util for the conversion." Returning the unpadded `cik_str` (which is what `company_tickers.json` provides) puts the burden on each caller.
- **Actual:** `cik = company.path("cik_str").asText("")` — stored as-is (e.g. `"320193"` for Apple). `SubmissionsClient.zeroPadCik` redoes the parse-and-pad on every call.
- **Why it matters:** Two formats co-exist in memory; easy to hand a non-padded CIK to a downstream module that expects padded form. The "small util" the doc suggests would centralise this — instead, normalization is split between `CikLookupService` (drops the format) and `SubmissionsClient.zeroPadCik` (re-adds it).
- **Proposed fix:** Either (a) make `CikLookupService` return the zero-padded form (the doc-canonical shape) and have `SecFiling.primaryDocumentUrl()` strip leading zeros, or (b) introduce a `CikFormats` util used by both sides. The doc explicitly endorses (b).

### 13. **Severity: Medium** — `CikLookupService` ignores share-class multiplicity; multiple tickers per CIK collapse silently
- **File:** `services/sec/CikLookupService.java:42-55`
- **Expected (SEC_API.md §3 "CIK uniqueness"):** "A single CIK can represent more than one ticker (multiple share classes, e.g., `GOOG` and `GOOGL`). The ticker map handles this — don't assume 1:1."
- **Actual:** The map is `Map<String,String>` ticker → CIK. There is no reverse map (CIK → tickers), and no signalling that one CIK has multiple tickers. The reverse direction is what `SecFiling.ticker()` ultimately needs (today it's pulled from the submissions response's `tickers[0]` list directly, which is OK — see `SubmissionsFilingExtractor.ticker`, but the class does not help disambiguate share classes either).
- **Why it matters:** When the firehose (deferred but documented) lands, you need CIK → ticker resolution and the choice of which class to emit matters. As built, this would need re-architecting.
- **Proposed fix:** Build `Map<String, List<String>> cikToTickers` and either return all classes or accept a preferred-class hint.

### 14. **Severity: Medium** — `SubmissionsFilingExtractor` picks `tickers[0]` blindly, ignoring `exchanges` and multi-class issuers
- **File:** `services/sec/SubmissionsFilingExtractor.java:90-95`
- **Expected (SEC_API.md §2 "Company tickers"):** Prefer `company_tickers_exchange.json` so OTC can be filtered; share classes are explicit. `SEC_IMPLEMENTATION.md` recommends emitting per share class.
- **Actual:**
  ```java
  private String ticker(SubmissionsResponse response) {
      if (response.tickers() == null || response.tickers().isEmpty()) {
          return "";
      }
      return response.tickers().get(0);
  }
  ```
  GOOG/GOOGL → always GOOG (whichever the SEC lists first). OTC tickers are not filtered.
- **Why it matters:** Wrong share class flowing downstream means analytics computes on the wrong OHLCV bars.
- **Proposed fix:** Either emit one candidate per ticker in the list, or prefer the highest-volume class via a CIK-class mapping table.

### 15. **Severity: Medium** — `SubmissionsClient.zeroPadCik` truncates to 10 digits only via `%010d`; rejects valid 11+ digit input
- **File:** `services/sec/SubmissionsClient.java:49-52`
- **Expected (SEC_API.md §3):** Always 10-digit zero-padded for the submissions URL.
- **Actual:**
  ```java
  private String zeroPadCik(String cik) {
      long numeric = Long.parseLong(cik.trim());
      return String.format("%010d", numeric);
  }
  ```
  - `Long.parseLong` throws `NumberFormatException` on a string that already includes a leading-zero CIK like `"CIK0000320193"` — caller must hand bare digits.
  - Throws on `null` (caller is `Objects.requireNonNull`-checked, but the message is misleading: `NumberFormatException` rather than a typed exception).
  - No fail-fast on negative numbers (would produce a sign-prefixed string from `%010d`).
- **Why it matters:** Brittle to callers that already have a zero-padded CIK in hand (e.g., from `CikLookupService` if finding #12 is fixed). Errors are stringly-typed.
- **Proposed fix:** Centralise CIK normalisation in a util that accepts bare digits, `"CIK0000…"`, or zero-padded forms; emits `IllegalArgumentException` with a descriptive message.

### 16. **Severity: Medium** — `SecFiling.primaryDocumentUrl()` blows up on a null / unparseable CIK at runtime
- **File:** `models/sec/SecFiling.java:31-38`
- **Expected:** Reconstruct a URL of the form `https://www.sec.gov/Archives/edgar/data/{CIK_no_leading_zeros}/{accession_no_dashes}/{primaryDocument}`.
- **Actual:**
  ```java
  public String primaryDocumentUrl() {
      String cikNoLeadingZeros = String.valueOf(Long.parseLong(cik));
      String accessionNoDashes = accessionNumber.replace("-", "");
      return "https://www.sec.gov/Archives/edgar/data/"
              + cikNoLeadingZeros + "/"
              + accessionNoDashes + "/"
              + primaryDocument;
  }
  ```
  - `Long.parseLong(cik)` on a malformed / null CIK throws at the moment a candidate's URL is read (often deep in serialisation or template rendering, not at ingestion time). The test `SecCandidateBuilderTest` (line 21) constructs `new SecFiling("00001234", …)` — note the CIK is sometimes set to plain `"1"` in `SecFilingScoringHeuristicsTest.eightK` — both work, but the function is not defensive.
  - `accessionNumber.replace("-","")` NPEs on null. No null guard.
  - `primaryDocument` not URL-encoded; SEC document names can contain spaces and accented characters.
- **Why it matters:** Data shapes coming from SEC will occasionally have surprises. URL construction should be defensive and encoded.
- **Proposed fix:** Add null guards (return `null` or `""` if essential fields are missing), URL-encode `primaryDocument`, and consider returning `Optional<URI>` instead of `String`.

### 17. **Severity: Medium** — `SecCandidateBuilder.tickerOrPlaceholder` substitutes the magic string `"UNKNOWN"` instead of dropping the candidate
- **File:** `services/sec/SecCandidateBuilder.java:112-116`
- **Expected (SEC_IMPLEMENTATION.md §3 "SecCandidate"):** `ticker` is `String ticker; // resolved; null if unresolvable`. The downstream design assumes a real ticker; an unresolvable CIK should either drop or be flagged explicitly.
- **Actual:**
  ```java
  private String tickerOrPlaceholder(SecFiling filing) {
      return filing.ticker() == null || filing.ticker().isBlank()
              ? "UNKNOWN"
              : filing.ticker();
  }
  ```
  Candidates with symbol `"UNKNOWN"` flow into `lib-marketdata` / `lib-analytics`, which will fail to fetch bars and emit `MISSING_SYMBOL` (per the failure-modes section of `LLM_TICKER_SCHEDULER.md` §12, that downstream behaviour is also expected). Better to never produce a candidate without a real symbol — the doc says "drop entries with no ticker (private filers, foreign with no US listing, etc.)" for Loop B; the principle holds for Loop A.
- **Why it matters:** Wastes downstream cycles, pollutes the review surface, and the `"UNKNOWN"` sentinel makes filtering harder.
- **Proposed fix:** Filter `null` / blank tickers in `SubmissionsFilingExtractor` (or `SecCandidateBuilder.process`) and log the dropped filing.

### 18. **Severity: Medium** — `scoreEarlyness` returns 0.90 for 0-hour-old filings but `filingDateToInstant` uses UTC midnight, so a filing logged on the same UTC date is always "less than 4 hours old" *before* 04:00 UTC and immediately stale after
- **File:** `services/sec/SecCandidateBuilder.java:118-153`
- **Expected:** `Instant observedAt` = "from filingDate (UTC midnight) or full timestamp if available" (`SEC_IMPLEMENTATION.md` §3). The current submissions endpoint does not return a wall-clock timestamp, only `filingDate`.
- **Actual:**
  ```java
  private Instant filingDateToInstant(LocalDate filingDate) {
      if (filingDate == null) { return Instant.now(clock); }
      return filingDate.atStartOfDay().toInstant(ZoneOffset.UTC);
  }
  ```
  Plus
  ```java
  long hoursSince = Math.max(0, (Instant.now(clock).getEpochSecond() - observedAt.getEpochSecond()) / 3600);
  if (hoursSince < 4)   return 0.90;
  ```
  Anchoring to UTC midnight makes the time-elapsed numbers off by up to a day for filings made during US market hours (filing at 4 PM ET on date D produces `observedAt = D 00:00 UTC`, which is 9 hours *before* the actual filing). The earlyness score therefore jumps a tier as soon as midnight UTC ticks over, regardless of when the document hit the wire.
- **Why it matters:** Materially distorts the early-stage signal for any filing made after ~8 PM UTC. The doc itself flags "or full timestamp if available" — there is no fallback to use the submissions-API `acceptanceDateTime` (which the SEC does provide but is not parsed today).
- **Proposed fix:** Either consume `acceptanceDateTime` from the submissions JSON (the SEC includes it in the `filings.recent` arrays) and use that, or change `scoreEarlyness` to use `LocalDate`-difference in days, not hours, so midnight-anchored timestamps don't cause artificial tier transitions.

### 19. **Severity: Medium** — `SubmissionsResponse` does not deserialize key fields the docs say we need (`isXBRL`, `isInlineXBRL`, `acceptanceDateTime`)
- **File:** `models/sec/SubmissionsRecentFilings.java:11-19`
- **Expected (SEC_API.md §2 "Key fields per filing"):** `accessionNumber`, `filingDate`, `form`, `primaryDocument`, `primaryDocDescription`, `items`, `isXBRL`, `isInlineXBRL`.
- **Actual:** Only six of the eight documented fields are parsed; `isXBRL`, `isInlineXBRL`, and the (SEC-provided) `acceptanceDateTime` are dropped. `@JsonIgnoreProperties(ignoreUnknown = true)` makes the omission silent.
- **Why it matters:** Without `acceptanceDateTime` you cannot fix finding #18. Without `isXBRL` you can't filter financial filings.
- **Proposed fix:** Extend the record with the missing list-typed columns.

### 20. **Severity: Medium** — `CikLookupService` is never used; `SubmissionsClient` requires the caller to provide a CIK directly
- **File:** `services/sec/CikLookupService.java`; `services/sec/SecIngestionService.java:40`
- **Expected (SEC_INGESTION.md §"Implemented today"):** "Submissions endpoint client. … In-scope form filtering …". (CIK-ticker map listed as not implemented yet.) However, the file `CikLookupService.java` exists and does the resolution — but `SecIngestionService.process(String cik)` accepts a CIK, not a ticker. There is no public method that accepts a ticker.
- **Actual:** `CikLookupService` is a dead service from the perspective of the in-module flow. Its dependency on `SecHttpClient` means it competes for the rate-limiter token whenever it is wired up, but it is currently called from nowhere inside `lib-ingestion`.
- **Why it matters:** Either (a) doc lies and code is ahead (then update README), or (b) code is half-finished and the lookup needs to be plumbed through. The `services/sec/README.md` "Reading order" table doesn't mention `CikLookupService` at all (line 14-23) and the wiring snippet (line 39-51) doesn't include it.
- **Proposed fix:** Add a `processByTicker(String ticker)` overload on `SecIngestionService` that uses `CikLookupService`; document it; add it to the reading order table.

### 21. **Severity: Medium** — `ManualCandidateIngestionService` has no input validation
- **File:** `services/ManualCandidateIngestionService.java:18-46`
- **Expected (lib-ingestion `README.md` "Testing Expectations"):** "duplicate behavior", "malformed or missing fields". The parent ARCHITECTURE invariants (and L1/L2 layering) imply at least basic shape validation.
- **Actual:** Accepts `null`/blank `symbol`, `companyName`, `headline`, `summary`; clamps nothing on the four scores (a caller can pass `-3.0` or `Double.NaN` and that flows downstream). No dedup against existing candidates (acknowledged in module README "duplicate behavior" expectations).
- **Why it matters:** Manual entry is the lowest-friction injection point for bad data into the pipeline. A NaN score will silently break downstream sorting.
- **Proposed fix:** Validate non-blank required strings, normalise `symbol` to uppercase, clamp the four `*Score` fields to `[0.0, 1.0]`, reject `NaN`/`Infinity`. Tests already exist for the happy path (`ManualCandidateIngestionServiceTest`) — extend them.

### 22. **Severity: Low** — Manual `candidateId` is a random UUID, defeating idempotency
- **File:** `services/ManualCandidateIngestionService.java:55-57`
- **Expected:** The whole point of `SecCandidateBuilder.candidateId = accessionNumber` is idempotency (per `SecCandidateBuilder` javadoc lines 38-42). Manual entries deserve the same property — re-submitting the same thesis should not create a new candidate.
- **Actual:**
  ```java
  private String candidateId() {
      return UUID.randomUUID().toString();
  }
  ```
  Two `process()` calls with identical inputs produce different ids → orchestrator dedup misses → two rows in the candidate table.
- **Why it matters:** A trader who refreshes the manual-entry form double-submits a thesis. Inconsistent with the SEC path's invariant.
- **Proposed fix:** Derive an id from a stable hash of `(symbol, headline, date)` or require the caller to supply an id. At minimum, document the divergence.

### 23. **Severity: Low** — `ManualCandidateIngestionService.observedAt()` uses `Instant.now()` directly with no `Clock` injection — untestable
- **File:** `services/ManualCandidateIngestionService.java:59-61`
- **Expected:** `SecCandidateBuilder` accepts an injected `Clock` (lines 47-65) precisely so tests can pin time. The manual service follows the same shape but does not get the same treatment.
- **Actual:** Hard-coded `Instant.now()`. The existing test cannot assert on `observedAt`.
- **Why it matters:** Inconsistent testability across the two ingestion paths.
- **Proposed fix:** Inject a `Clock` matching `SecCandidateBuilder`.

### 24. **Severity: Low** — `SubmissionsFilingExtractor` `FORMS_IN_SCOPE` membership uses substring-insensitive `equals`; some real SEC form variants slip
- **File:** `services/sec/SubmissionsFilingExtractor.java:20-28, 68-70`
- **Expected (SEC_FORMS.md §1):** S-1, S-3, 424B (with variants), 13D/13G — note `13D/A` (amendments) and `424B1`–`424B5` are real form values from the SEC.
- **Actual:**
  ```java
  private static final Set<String> FORMS_IN_SCOPE = Set.of(
          "8-K", "4", "SC 13D", "SC 13G", "S-1", "S-3", "424B");
  ...
  private boolean isInScope(String formType) {
      return formType != null && FORMS_IN_SCOPE.contains(formType.trim());
  }
  ```
  - `SC 13D/A` (amendment) → dropped, even though `SEC_FORMS.md` §4 explicitly says "13D/A — amendment to a previously filed 13D. Same parser."
  - `424B1`, `424B2`, `424B3`, `424B4`, `424B5` (the real prospectus form codes) → all dropped. Pure `"424B"` rarely appears on the wire.
  - `4/A` (amended Form 4) → dropped.
  - `8-K/A` (amended 8-K) → dropped.
- **Why it matters:** The matcher silently misses a meaningful slice of in-scope filings.
- **Proposed fix:** Move from exact match to prefix-match with explicit handling:
  ```java
  if (form.equals("8-K") || form.startsWith("8-K/A")) return true;
  if (form.equals("4") || form.equals("4/A")) return true;
  if (form.startsWith("SC 13D") || form.startsWith("SC 13G")) return true;
  if (form.equals("S-1") || form.equals("S-3")) return true;
  if (form.startsWith("424B")) return true;
  ```

### 25. **Severity: Low** — 8-K item code 9.01 is treated as a real category candidate but the doc says it's meaningless standalone
- **File:** `services/sec/EightKCategoryMapper.java:32-44` and `SubmissionsFilingExtractor.java:113-122`
- **Expected (SEC_FORMS.md §2 Notes):** "Item 9.01 alone is meaningless — it just declares attached exhibits. Always pair it with another Item."
- **Actual:** An 8-K with only `["9.01"]` falls through to `FILING_EVENT` with the same score as no items at all. No explicit detection-and-drop / detection-and-log of "9.01 alone".
- **Why it matters:** Doc-stated invariant is not enforced. Mostly cosmetic but the doc explicitly flagged the case.
- **Proposed fix:** In `EightKCategoryMapper`, strip `9.01` from the input list at the top; if empty, fall back to `FILING_EVENT` and log "9.01 standalone".

### 26. **Severity: Low** — `EightKCategoryMapper`'s priority order does not match the heuristic table in `SEC_FORMS.md` §"Suggested first-pass classification"
- **File:** `services/sec/EightKCategoryMapper.java:37-43`
- **Expected:** Doc says "Conflicts (e.g., a filing with both `2.02` and `5.02`) get the higher-signal category; document the tiebreak rules so they're auditable."
- **Actual:** The order is: severe → M&A (→CONTRACT) → contract → dilution → earnings → governance → default. This is fine on its face, but:
  - Dilution (3.02/3.03) is ranked above earnings (2.02). Is dilution always higher signal than earnings? The doc doesn't pick a winner.
  - Governance (5.02) is last, but `5.02` (CEO departure) is often higher-impact than a generic earnings beat.
  - There is no code-level comment justifying these tiebreaks; the docstring (lines 14-16) says "based on the highest-signal item present" but does not enumerate.
- **Why it matters:** Doc requires auditable rules; the rules are implicit.
- **Proposed fix:** Add a comment block at the top of `process()` enumerating the priority chain and the rationale, and write a test that pins down each tie.

### 27. **Severity: Low** — `SecCandidateBuilder.scoreStructuralReality` `default` branch is unreachable
- **File:** `services/sec/SecCandidateBuilder.java:138-144`
- **Actual:**
  ```java
  private double scoreStructuralReality(CandidateCatalystType catalystType) {
      return switch (catalystType) {
          case STRUCTURAL_DEMAND_CHANGE, CONTRACT -> 0.90;
          case FILING_EVENT                       -> 0.75;
          default                                 -> 0.70;
      };
  }
  ```
  `pickCatalystType` only ever returns `STRUCTURAL_DEMAND_CHANGE`, `CONTRACT`, or `FILING_EVENT` — the `default` branch can never fire. Dead code.
- **Why it matters:** Misleading reader into thinking other catalyst types can come through.
- **Proposed fix:** Either remove the `default` arm or extend `pickCatalystType` to use the broader enum (after finding #1 is addressed).

### 28. **Severity: Low** — `SubmissionsClient` constructs a new `ObjectMapper` per instance; not Spring-managed, not module-shared
- **File:** `services/sec/SubmissionsClient.java:30-32` and `CikLookupService.java:23-30`
- **Expected:** A single Spring-managed `ObjectMapper` should be shared (Jackson recommends one instance per app).
- **Actual:** Each service has its own `ObjectMapper`, constructed lazily, with no configuration.
- **Why it matters:** Wastes memory; harder to add JavaTime modules, Kotlin modules, or a configured `JsonNamingStrategy` consistently.
- **Proposed fix:** Inject the autowired Spring `ObjectMapper` (Spring Boot supplies one) — drop the no-arg fallback constructors.

### 29. **Severity: Low** — `EightKCategoryMapperTest` validates the wrong invariant for M&A
- **File:** `src/test/java/dev/reddragon/ingestion/services/sec/EightKCategoryMapperTest.java:41-43`
- **Test:**
  ```java
  @Test
  void mna_takes_priority_over_contract() {
      assertEquals(CandidateCatalystType.CONTRACT, mapper.process(List.of("2.01", "1.01")));
  }
  ```
  The test *name* says "M&A takes priority over contract" but asserts `CONTRACT` — because both arms map to `CONTRACT`. The test passes vacuously and locks in finding #1.
- **Why it matters:** False confidence; the assertion does not in fact test what the name says.
- **Proposed fix:** Once finding #1 is fixed, change the assertion to `MERGER_AND_ACQUISITION` (or whatever the new enum value is).

### 30. **Severity: Low** — `services/sec/README.md` reading-order table omits `CikLookupService` and `SecFilingScoringHeuristics`
- **File:** `src/main/java/dev/reddragon/ingestion/services/sec/README.md:13-23`
- **Expected:** Reading-order should cover every class in the package. Both `CikLookupService` and `SecFilingScoringHeuristics` are present in source but not listed.
- **Actual:** Reading-order stops at `SecCandidateBuilder`. `SecFilingScoringHeuristics` is only mentioned once in the wiring block (line 46-47); `CikLookupService` is not mentioned at all.
- **Proposed fix:** Add both to the table with a one-line `process()` description.

---

## Doc / Code Drift

### Documented but not implemented

- **LLM Ticker Scheduler** (entire `LLM_TICKER_SCHEDULER.md`). Nothing in
  `services/llm/`, `models/llm/`, `config/Llm*Properties.java`, or
  `Flyway V4__llm_audit.sql` exists. `SourceType` is missing `LLM_SUGGESTION`.
  Zero classes from the design doc's §4 file layout are present.
- **SEC RSS firehose / Loop B** — `SEC_API.md` §"Latest-filings RSS",
  `SEC_IMPLEMENTATION.md` §2 "Loop B". Not implemented (acknowledged in
  `SEC_INGESTION.md`).
- **HTTP backoff / retry on 429-503** — `SEC_API.md` §1 "Backoff". Acknowledged
  open gap; see finding #4.
- **`SourceType` SPI / `Candidate` interface / sealed `SecPayload`** —
  `SEC_IMPLEMENTATION.md` §3. No generic `Source` / `Candidate` interfaces;
  `TradeCandidate` is used directly.
- **Per-CIK polling cursor + global dedup store** — `SEC_IMPLEMENTATION.md`
  §2 "State to persist between polls". `SecIngestionService` is stateless;
  README says "caller's responsibility".
- **CIK-ticker cache TTL / fail-stale** — `SEC_API.md` §2, `SEC_IMPLEMENTATION.md`
  §6.2. `CikLookupService` loads once-and-forever (finding #11).
- **Document caching to local filesystem** — `SEC_IMPLEMENTATION.md` §6.3.
  Not implemented.
- **EFTS full-text search** — `SEC_API.md` §2. Deferred but no scaffolding.
- **Daily index file backfill** — `SEC_API.md` §2. Deferred but no scaffolding.
- **Form 4 XML body parser** — `SEC_FORMS.md` §3. Not implemented; only index
  metadata is consumed.
- **13D/13G HTML body parser** — `SEC_FORMS.md` §4. Not implemented.
- **S-1 / S-3 / 424B body extraction** (aggregate offering size) —
  `SEC_FORMS.md` §1 / `SEC_IMPLEMENTATION.md` §3 `OfferingPayload`. Not
  implemented.
- **News / RSS / scanner / macro adapters** — `README.md` "Planned Or Deferred".
  Not implemented.
- **HTTP timeouts and Spring `@ConfigurationProperties` on `SecApiProperties`**
  — implied by `SEC_API.md`'s "config-driven, fail-fast on startup". Neither.
- **Form 3, Form 5** — `SEC_FORMS.md` §1 (recommended drop initially). Code
  matches the recommended drop; no drift.

### Implemented but not documented

- `CikLookupService` — exists and works, but not wired into the public ingestion
  flow; absent from `services/sec/README.md` reading-order table (finding #20, #30).
- `SecFilingScoringHeuristics` — recently introduced (the javadoc lines 19-21
  say "explicit replacement for the `return 0.50;` placeholders"). Mentioned in
  the wiring snippet but not in the `services/sec/README.md` reading-order.

### Doc inconsistencies

- `SEC_INGESTION.md` line 21 says "in-scope forms: `8-K`, `4`, `SC 13D`,
  `SC 13G`, `S-1`, `S-3`, `424B`" — matches the code's `FORMS_IN_SCOPE` set
  exactly, but neither doc nor code mentions amendment suffixes (`/A`).
- `SEC_INGESTION.md` line 22 promises "8-K item-code mapping to internal
  catalyst categories" — true, but the categories the mapper produces
  (`STRUCTURAL_DEMAND_CHANGE`, `CONTRACT`, `FILING_EVENT`) are not the
  seven categories `SEC_FORMS.md` §2 enumerates (`earnings`, `M&A`,
  `contract`, `governance`, `severe-negative`, `dilution`, `disclosure`,
  `other`). The mapping from "doc category" to "domain enum" is unstated.
- `services/sec/README.md` line 23 references `SecCandidateBuilder` as the
  last class but doesn't mention that `SecCandidateBuilder` delegates scoring
  to `SecFilingScoringHeuristics`.

---

## Strengths

- Clean **single-responsibility / one-`process()`** convention is followed
  consistently across `services/sec/`. The reading-order in
  `services/sec/README.md` accurately reflects the call graph.
- **Idempotency via accession number** as `candidateId` in
  `SecCandidateBuilder.candidateId` (line 98-100) is a thoughtful, doc-supported
  design choice — exactly the user's stated invariant about ingestion
  idempotency.
- **`Clock` injection** in `SecCandidateBuilder` (line 47-65) makes earlyness
  scoring deterministically testable; `SecCandidateBuilderTest.earlynessScoreDropsAsFilingAges`
  exercises this well.
- **`SecFilingScoringHeuristics` is a pure function**, well-commented, with
  explicit doc-aligned tiebreaks (lines 36-93). Replacing the previous
  `0.50` placeholders was the right move.
- **`@JsonIgnoreProperties(ignoreUnknown = true)`** on all three SEC JSON
  records — defensive against the SEC adding fields.
- **`SimpleRateLimiter` validates `requestsPerSecond > 0`** at construction;
  smoke test covers it.
- **`SubmissionsFilingExtractor` correctly handles null `recent` block**
  (line 37-39, test `nullRecentBlockReturnsEmpty`).
- The implementation is genuinely a **thin SEC client**, per the project's
  decision to decline `edgar4j` — no library bloat, no XML parsing imported
  prematurely. The user's memory notes that this was the explicit goal; the
  code matches it.
- Test coverage is **proportional and targeted** to the implemented surface
  — no aspirational tests for un-built features, no over-mocked unit tests.

---

## Summary

| Severity | Count |
| -------- | ----- |
| Critical | 0     |
| High     | 7     |
| Medium   | 14    |
| Low      | 9     |
| **Total**| **30**|

**Headline:** No critical issues. The most impactful single gap is the
**absence of HTTP retry / backoff + timeouts** in `SecHttpClient` (#4, #5),
followed by the **8-K item taxonomy under-coverage** (#1, #2, #3) that
silently downgrades the most common press-release shape (`7.01` / `8.01`)
and the genuine impairment / debt items (`2.03`, `2.05`, `2.06`) to the
lowest-signal residual category. The `SimpleRateLimiter` (#7) is also
operationally bursty in a way that risks tripping SEC fair-access on
the second boundary. The entire `LLM_TICKER_SCHEDULER.md` design is
documented in detail but not yet implemented.
