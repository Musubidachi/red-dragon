# SEC API — Rules and Endpoints

Reference for anyone implementing the HTTP client that talks to SEC EDGAR.
For what to do with the responses, see [SEC_FORMS.md](./SEC_FORMS.md).
For how the client fits into the pipeline, see [SEC_IMPLEMENTATION.md](./SEC_IMPLEMENTATION.md).

---

## 1. Rules of the road

These are not optional. Violating them gets your IP rate-limited or banned.

### User-Agent

Every request to `*.sec.gov` MUST include a `User-Agent` header that identifies
the requester and provides a monitored contact email. Format the SEC accepts:

```
User-Agent: red-dragon (your-real-name@your-domain.com)
```

- Do **not** use `example.com`, `noreply@`, or fake emails.
- Do **not** use a generic browser UA string.
- Do **not** ship without setting this — config-driven, fail-fast on startup if missing.

The User-Agent must be configurable via environment variable / Spring property,
**not** hardcoded.

### Rate limit

- **≤ 10 requests per second** across all SEC hosts (`www.sec.gov`, `data.sec.gov`).
- This is a global limit per IP, not per-endpoint.
- Use a token-bucket limiter with capacity 10, refill 10/sec. Be conservative —
  start at 5/sec to leave headroom for retries.

### Compression and encoding

- All responses support gzip. Send `Accept-Encoding: gzip, deflate`.
- Spring's `RestClient` with default config handles decompression automatically.

### TLS

- HTTPS only. The SEC redirects HTTP to HTTPS but don't rely on that.

### Hosts

| Host                  | Used for                                                   |
| --------------------- | ---------------------------------------------------------- |
| `www.sec.gov`         | Filing archives, ticker mapping JSON, RSS feeds, search    |
| `data.sec.gov`        | Submissions API (per-company filing history), Frames API   |
| `efts.sec.gov`        | Full-text search (EDGAR Full-Text Search API)              |

### Backoff

On `429 Too Many Requests` and `503`: exponential, jittered, max 5 retries,
max delay 30s. After that, fail the poll and try again on the next scheduled
tick. The 10/s budget is global per IP, so retries draw from the same bucket.

---

## 2. Endpoints we will use

### Company tickers (CIK ↔ ticker map)

```
GET https://www.sec.gov/files/company_tickers.json
```

Returns a JSON object keyed by index, each entry has `cik_str`, `ticker`, `title`.
Refresh weekly; cache in memory with TTL. This is the only way to translate a
CIK in a filing into a ticker we recognize.

There's also `company_tickers_exchange.json` which adds the listing exchange.
Prefer that one — exchange info is useful for filtering (e.g., skip OTC).

### Submissions API (per-company filing history)

```
GET https://data.sec.gov/submissions/CIK{10-digit-zero-padded}.json
```

Example for Microsoft (CIK 789019):

```
https://data.sec.gov/submissions/CIK0000789019.json
```

Returns a JSON object with the company's recent filings (up to ~1000) under
`filings.recent`. Older filings are paginated via `filings.files[]`.

Key fields per filing in `filings.recent` (column-oriented arrays — index `i`
in each array is the same filing):

- `accessionNumber[i]`
- `filingDate[i]` (YYYY-MM-DD)
- `form[i]` (e.g., `"8-K"`, `"4"`, `"SC 13D"`)
- `primaryDocument[i]`
- `primaryDocDescription[i]`
- `items[i]` (8-K only — comma-separated list of Item codes, e.g. `"1.01,9.01"`)
- `isXBRL[i]`, `isInlineXBRL[i]`

This is the workhorse. If you're watching a fixed list of tickers, the loop is:
ticker → CIK → poll submissions JSON → diff against last seen → emit candidates.

### Primary document URL

Constructed from the submissions response:

```
https://www.sec.gov/Archives/edgar/data/{CIK_no_leading_zeros}/{accession_no_dashes}/{primaryDocument}
```

### Full-text search (EFTS)

```
GET https://efts.sec.gov/LATEST/search-index?q=...&forms=8-K&dateRange=custom&startdt=...&enddt=...
```

Returns JSON results. Useful for keyword surfacing across all filings (e.g.,
"government contract", "FDA approval"). Out of scope for the first pass —
the per-CIK polling is enough.

### Latest-filings RSS (firehose)

```
GET https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&type=&output=atom
```

Returns the most recent filings across all of EDGAR as an Atom feed. Filter
by `type=8-K` to narrow. Good for watching for filings on tickers **outside**
your watchlist. Polling cadence: every 1–5 minutes is plenty.

This is how you catch a Form 4 insider buy on a name you weren't already
watching, which is a real source of asymmetric ideas.

### Daily index files

```
https://www.sec.gov/Archives/edgar/full-index/{YYYY}/QTR{1-4}/master.idx
https://www.sec.gov/Archives/edgar/full-index/{YYYY}/QTR{1-4}/form.idx
```

Pipe-delimited, one row per filing. Only useful for historical backfill or
batch processing — not for live ingestion. Defer.

---

## 3. Format gotchas

### Two CIK formats in URLs

- Submissions endpoint: 10-digit zero-padded (`CIK0000789019`)
- Archives endpoint: stripped of leading zeros (`/data/789019/`)

Build a small util for the conversion.

### Two accession-number formats

- In metadata (submissions JSON, search results): with dashes
  (`0001193125-23-262357`)
- In URLs (Archives paths): without dashes (`000119312523262357`)

Same util can handle both.

### CIK uniqueness

A single CIK can represent more than one ticker (multiple share classes,
e.g., `GOOG` and `GOOGL`). The ticker map handles this — don't assume 1:1.

---

## 4. References

- SEC EDGAR API docs: <https://www.sec.gov/edgar/sec-api-documentation>
- SEC Fair Access policy (rate limits + UA): <https://www.sec.gov/os/accessing-edgar-data>
- EDGAR full-text search UI (for sanity checks): <https://efts.sec.gov/LATEST/search-index?q=&forms=8-K>
- Latest filings firehose (Atom): `https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&output=atom`
