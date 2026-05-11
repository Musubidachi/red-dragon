# SEC Ingestion — Implementation Plan

How the SEC source adapter fits into `lib-ingestion` and the rest of the
pipeline. For the HTTP-side rules, see [SEC_API.md](./SEC_API.md). For form-by-form
parsing details, see [SEC_FORMS.md](./SEC_FORMS.md).

---

## 1. Purpose

`lib-ingestion` exposes one or more `Source` adapters that produce a stream of
`Candidate` records for the downstream pipeline. The SEC adapter is the first
source. Its job is narrow:

- Watch for newly filed forms on the SEC EDGAR system.
- Filter to forms that carry catalyst signal for a discretionary trader.
- Resolve CIK → ticker.
- Emit a structured `SecCandidate` with a small per-form payload.

It does **not** score, filter for trade-worthiness, parse XBRL financials,
persist to disk, or do anything portfolio-aware. Those belong to other modules.

---

## 2. Polling strategy

The SEC doesn't push. We poll. Two complementary loops:

### Loop A — watchlist polling (per-CIK submissions endpoint)

- Input: a list of tickers we care about. Source for this list is an open
  decision (see §6).
- Resolve each to its CIK once via `company_tickers.json` (cached in memory).
- For each CIK on a configured cadence (every 5–15 min during market hours,
  hourly off-hours), fetch the submissions JSON.
- Diff against the last `accessionNumber` we saw; emit a `SecCandidate` for
  each new filing that matches our scope.

### Loop B — firehose RSS (latest filings)

- Poll `getcurrent` Atom feed every 1–5 min.
- Filter to our scoped form types.
- Resolve CIK → ticker via the cached map; drop entries with no ticker (private
  filers, foreign with no US listing, etc.).
- Emit candidates the same way Loop A does. Loop A and Loop B may both surface
  the same filing; downstream dedupes by `accession_number`.

Loop B is what catches a Form 4 buy on a stock you didn't know to watch.

### State to persist between polls

- Per-CIK: last `accessionNumber` seen (so we don't re-emit on every poll).
- Global: a small dedupe set of accession numbers seen recently (last N days).

The state itself goes through `lib-persistence`; `lib-ingestion` only reads/writes
through that interface.

---

## 3. Data model — `SecCandidate`

Suggested record shape (Java 21). This is the contract `lib-ingestion`
emits to the rest of the pipeline.

```java
record SecCandidate(
    String accessionNumber,       // "0001193125-23-262357"
    String cik,                   // 10-digit zero-padded
    String ticker,                // resolved; null if unresolvable
    String formType,              // "8-K", "4", "SC 13D", "S-3", ...
    Instant filedAt,              // from filingDate (UTC midnight) or full timestamp if available
    URI primaryDocumentUrl,
    SecPayload payload            // sealed interface; one variant per form family
) implements Candidate { }

sealed interface SecPayload
    permits Form8KPayload, Form4Payload, Schedule13Payload, OfferingPayload, GenericPayload {}

record Form8KPayload(
    List<String> itemCodes,
    String catalystCategory       // "earnings", "M&A", "contract", "governance", "severe-negative", "dilution", "disclosure", "other"
) implements SecPayload {}

record Form4Payload(
    String reportingOwnerName,
    String reportingOwnerRelationship,   // "Officer", "Director", "10%+", or composite
    List<Form4Transaction> transactions,
    boolean is10b51Plan
) implements SecPayload {}

record Form4Transaction(
    LocalDate transactionDate,
    String code,                  // "P", "S", "A", ...
    String acquiredDisposed,      // "A" or "D"
    BigDecimal shares,
    BigDecimal pricePerShare,
    BigDecimal sharesOwnedAfter
) {}

record Schedule13Payload(
    String filerName,
    BigDecimal pctOfClass,
    BigDecimal sharesOwned,
    boolean isActivist            // 13D vs 13G
) implements SecPayload {}

record OfferingPayload(
    String formType,
    String aggregateOffering      // if extractable
) implements SecPayload {}

record GenericPayload(String formType) implements SecPayload {}
```

The shared `Candidate` interface (probably defined alongside the source SPI in
`lib-ingestion`) is the boundary the rest of the pipeline depends on. Other
sources (news, scanners, macro) implement their own `Payload` variants but
emit through the same `Candidate` channel.

---

## 4. Rate limiting — Java approach

Two reasonable choices:

**Option A — Bucket4j** (`com.bucket4j:bucket4j-core`).
Idiomatic, battle-tested, expressive. Adds one dep. Pin to a specific version
in the parent pom. Use a single shared `Bucket` for all SEC requests.

**Option B — hand-rolled `Semaphore` + `ScheduledExecutorService`.**
Refill 10 permits every second. Each request acquires one. Roughly 30 lines.
Zero deps, easy to read.

For a single-tenant personal tool, Option B is fine. For anything that might
later add concurrent feeds or multi-tenant isolation, Option A scales better.

In either case:

- Wrap every SEC HTTP call in the limiter — including ticker-map fetches and
  any retries.
- The 10/sec is a *global* per-IP limit, not per-endpoint.
- Run the limiter conservatively (e.g., 5/sec) to leave room for retry storms.

---

## 5. Out of scope (explicit)

These are deliberately not done in `lib-ingestion`:

- **XBRL parsing.** Don't try to read 10-K/10-Q financials. We don't need them.
- **Persistence.** All storage goes through `lib-persistence` interfaces.
- **Scoring, regime fit, asymmetry math.** That's `lib-analytics`.
- **Hard-rule filtering, verdict.** That's `lib-validation`.
- **Order placement, portfolio awareness.** Out of scope for the platform until
  an execution layer is added.
- **News/RSS, market scanners, macro feeds.** Separate `Source` adapters in
  this same module, but each is its own implementation effort.

---

## 6. Open decisions (deferred to implementation)

1. **Watchlist source.** Hardcoded list, JSON config file, or DB-backed table?
   - Recommend: JSON config to start (`application-secsource.yml` or a small
     `tickers.json` resource), promote to DB-backed later. Avoid building a CRUD
     UI for it before there's pressure to.

2. **CIK ↔ ticker cache.** In-memory, refreshed weekly, with a fail-stale policy
   (if refresh fails, keep using the last known map and log)?
   - Recommend: yes. Persisting it in `lib-persistence` is overkill for a
     ~3 MB JSON map.

3. **Document caching.** Do we save fetched primary documents anywhere, or
   re-fetch on demand?
   - Recommend: cache to local filesystem (`./data/sec-cache/{accession}/{primaryDoc}`)
     keyed by accession number. Filings are immutable once filed; cache forever.
     Path is configurable.

4. **What we do with filings on tickers not in the watchlist** (Loop B).
   - Drop, emit, or rate-limit? Default to *emit* but downstream validation
     can drop them quickly. The asymmetric idea source value is real.

5. **Form 5 / Form 3 — emit or drop?**
   - Recommend: drop initially. Add if/when we miss something.

6. **Operating-hours awareness.**
   - Should polling cadence change between RTH, after-hours, weekends? Probably
     yes for cost reasons but not for correctness — defer.

---

## 7. Suggested implementation order

If picking this up cold, do it in this order:

1. **SPI**: define `Source`, `Candidate`, and the `SecCandidate` / `SecPayload`
   types. No implementation yet; just the surface.
2. **Rate limiter + `SecHttpClient` wrapper** that handles UA, gzip, backoff,
   and the 10/s budget. Test it against `company_tickers.json` (one harmless
   request).
3. **CIK-ticker map loader** with weekly refresh + in-memory cache.
4. **Submissions API client**: given a CIK, return parsed `SubmissionsResponse`.
5. **Loop A** (watchlist polling) end-to-end, emitting `SecCandidate` for 8-Ks
   only — simplest payload to start.
6. **8-K Item-code → catalyst category mapper**.
7. **Form 4 XML parser** + Form 4 candidates.
8. **13D/G HTML parser** + Schedule 13 candidates.
9. **Loop B** (Atom firehose) for cross-watchlist surfacing.
10. **Offerings** (S-1/S-3/424B) — last, partly because the body parsing is
    annoying and partly because the signal is mostly "this exists, dilution
    risk, defer to validator."

Each step is independently testable; don't conflate them.

---

## 8. References

For SEC-side endpoint details and rules, see [SEC_API.md](./SEC_API.md).
For form-by-form parsing notes, see [SEC_FORMS.md](./SEC_FORMS.md).
