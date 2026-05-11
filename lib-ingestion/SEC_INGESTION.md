# SEC EDGAR Ingestion

Index for the SEC ingestion design docs. Read these before implementing the
SEC source adapter inside `lib-ingestion`.

| Doc                                              | What's in it                                                                 |
| ------------------------------------------------ | ---------------------------------------------------------------------------- |
| [SEC_API.md](./SEC_API.md)                       | API rules of the road (User-Agent, rate limit, TLS, hosts) and the specific endpoints we'll call, with URL formats and the CIK/accession-number gotchas. |
| [SEC_FORMS.md](./SEC_FORMS.md)                   | Which SEC forms are in scope and why; full 8-K Item-code taxonomy; Form 4 XML structure with transaction-code signal table; 13D/G parsing notes. |
| [SEC_IMPLEMENTATION.md](./SEC_IMPLEMENTATION.md) | Module purpose; two-loop polling strategy; `SecCandidate` data model; rate-limiter options; open decisions; explicit out-of-scope; suggested implementation order. |

## Quick orientation

`lib-ingestion` exposes `Source` adapters that produce a stream of `Candidate`
records. The SEC adapter is the first source. Its narrow job: watch for newly
filed forms, filter to forms that carry catalyst signal, resolve CIK → ticker,
emit structured `SecCandidate` records.

It does **not** score, persist, parse XBRL financials, or know about a portfolio.
Those belong to other modules.

## Starting points

- If you're writing the HTTP client → start with [SEC_API.md](./SEC_API.md).
- If you're writing the parsers → start with [SEC_FORMS.md](./SEC_FORMS.md).
- If you're wiring the polling/orchestration → start with [SEC_IMPLEMENTATION.md](./SEC_IMPLEMENTATION.md).

## Top-level references

- SEC EDGAR API docs: <https://www.sec.gov/edgar/sec-api-documentation>
- SEC Fair Access policy (rate limits + UA): <https://www.sec.gov/os/accessing-edgar-data>
