# lib-ingestion — LLM Ticker Scheduler

A design doc for an L1 (Opportunity Discovery) source that asks multiple LLMs,
on a daily schedule, to nominate tickers that are likely to pass red-dragon's
validation. Each nomination becomes a `TradeCandidate` and enters the same
ingestion → enrichment → analytics → validation pipeline as SEC filings and
manual entries.

This is not yet implemented. It lives next to `SEC_INGESTION.md` so the
design is reviewable before code lands.

## 1. Purpose

SEC EDGAR is a high-precision but narrow funnel — it surfaces opportunities
only when a filing crosses the wire. Most disequilibrium opportunities
described by the trading framework (policy-driven capital flows, sector
rotation, structural supply constraints) don't produce a single filing event.
They show up across news, sector chatter, macro shifts, and lateral inference
that an LLM with a fresh enough world model can do well.

The LLM Ticker Scheduler exists to widen the candidate funnel without lowering
its quality. The trick is in the prompt: we don't ask "what's interesting" —
we ask "what's likely to pass red-dragon's seven-dimension validation."

## 2. Where it fits

MD Layer 1 (Opportunity Discovery). It is a peer of the existing SEC ingestor.
Both modules emit `TradeCandidate` through the same constructor, with different
`SourceType` values and slightly different score-hint shapes.

```
[ SEC EDGAR    ]  ───┐
[ Manual entry ]  ───┤
[ LLM scheduler ] ───┴──> TradeCandidate ──> market enrich ──> analytics ──> validation
```

Nothing downstream knows or cares that the candidate originated from an LLM.
The validation engine treats it identically. The trader sees the source in
`candidate.sourceType()` on the review surface.

## 3. Core idea — multiple LLMs, consensus weighting

Single-source LLM suggestions inherit the source LLM's biases. A model trained
on heavy financial-media diet over-weights momentum names; a model with a
recent knowledge cutoff over-weights yesterday's news; a model with strong
disclaimers refuses to name tickers at all.

We mitigate by querying ≥3 LLMs in parallel with the same prompt and
**aggregating with consensus weighting**:

- A ticker mentioned by **≥2 LLMs** lands in the daily candidate set with
  a confidence multiplier proportional to the count.
- A ticker mentioned by **only 1 LLM** is still ingested, but flagged
  `singleSource = true` and reviewed only if the validation score is high.
- Per-LLM **historical reliability weights** (set by the trader after a
  calibration period) multiply each LLM's vote so a track-record-strong
  model beats a track-record-weak one when they disagree.

## 4. Where in the module

```
lib-ingestion/src/main/java/dev/reddragon/ingestion/
    services/llm/
        LlmTickerScheduler.java        — @Scheduled cron entry
        LlmTickerIngestionService.java — orchestrator (per-day flow)
        LlmTickerAggregator.java       — consensus / per-LLM weighting
        TickerPromptBuilder.java       — assembles the prompt
        TickerSuggestionParser.java    — parses the JSON the LLM returns
        provider/
            LlmProvider.java                  — minimal interface
            ClaudeLlmProvider.java            — adapter
            OpenAiLlmProvider.java            — adapter
            GeminiLlmProvider.java            — adapter
            MockLlmProvider.java              — test-mode adapter (fixed payload)
    models/llm/
        LlmTickerSuggestion.java       — one ticker as suggested by one LLM
        LlmResponseEnvelope.java       — Jackson DTO for the LLM JSON
        LlmAggregatedSuggestion.java   — consensus-weighted output
    config/
        LlmProviderProperties.java     — shared properties (model, temp, max tokens)
        ClaudeLlmProperties.java       — per-provider API key + endpoint
        OpenAiLlmProperties.java
        GeminiLlmProperties.java
```

New enum value:

```
SourceType.LLM_SUGGESTION
```

## 5. The prompt — the heart of the design

The prompt does three things at once: it teaches the LLM what red-dragon is
looking for, constrains the search space, and forces structured output.

### 5.1 System prompt (constant)

```
You are an opportunity-discovery assistant for a discretionary US-equities
trader who runs a probabilistic market-state platform called red-dragon.

red-dragon's edge is identifying TEMPORARY DISEQUILIBRIUM before the broader
market restores equilibrium. It validates each candidate on seven dimensions:

  1. Structural reality       — Is the catalyst real, factual, sourced?
  2. Material significance    — Does it actually move the company's value?
  3. Earlyness                — Is the narrative still pre-saturation?
  4. Equilibrium quality      — Is the regime supportive of mean reversion
                                or persistent expansion?
  5. Reflexivity potential    — Will participation amplify the move?
  6. Asymmetry                — Is the remaining risk/reward favorable?
  7. Regime compatibility     — Does the broader market support deployment?

red-dragon REJECTS:
  - already-fully-repriced names
  - hype without structural backing
  - illiquid micro-caps (<$50M float)
  - meme momentum chasing
  - any name in active short-squeeze
  - crypto, OTC pink sheets, SPACs without target
  - anything outside Roth-IRA-permissible US listings

The trader is patient and selective. They prefer ONE high-conviction setup
over ten weak ones. Default to fewer suggestions, not more.

You are not a financial advisor. You are a discovery assistant.
```

### 5.2 User prompt (built daily by `TickerPromptBuilder`)

```
Today's date: {{TODAY}}.
Yesterday's macro context: {{OPTIONAL_MACRO_NOTE}}.
The trader is currently focused on: {{FOCUS_AREAS}}.        // configurable
Tickers already in review this week (exclude): {{EXCLUSIONS}}.

Suggest up to {{MAX_TICKERS}} US-listed equity tickers that are likely to
pass red-dragon's seven-dimension validation TODAY.

For each ticker, provide your honest assessment on the seven dimensions
as 0.00–1.00 hints. These are HINTS, not commitments — red-dragon will
compute its own scores from market data. We use your hints only for
sorting and tie-breaking.

If you cannot identify any tickers that meet a credible bar today, return
an empty list. Quality over quantity. The trader would rather see zero
suggestions than five mediocre ones.

Return ONLY valid JSON matching this schema:

{
  "asOf": "ISO-8601 UTC",
  "tickers": [
    {
      "symbol":                    "TICKER",
      "catalystType":              one of GOVERNMENT_GRANT | POLICY_CHANGE |
                                          CONTRACT | SUPPLY_CONSTRAINT |
                                          SECTOR_INCENTIVE | LIQUIDITY_SHIFT |
                                          STRUCTURAL_DEMAND_CHANGE | NEWS_EVENT,
      "structuralRealityHint":     0.00–1.00,
      "materialSignificanceHint":  0.00–1.00,
      "earlynessHint":             0.00–1.00,
      "reflexivityHint":           0.00–1.00,
      "asymmetryHint":             0.00–1.00,
      "thesis":                    "one paragraph, concrete",
      "primarySourceUrl":          "if you can cite one"
    }
  ]
}
```

### 5.3 Why the hints are non-binding

red-dragon does its own scoring downstream. The LLM's hints are stored on
the candidate as `structuralRealityScore`, `materialSignificanceScore`, etc.
only so the daily review surface can sort by LLM-perceived conviction. The
authoritative scores come from `DeterministicAnalyticsService` and
`DisequilibriumValidationEngine` after the candidate is enriched with real
market data.

This separation matters: an LLM that hallucinates a 0.95 structural-reality
score for a fictional government grant will be caught when the validation
engine finds no supporting filing and the analytics layer sees no volume
expansion.

## 6. Response parsing & defensive handling

LLMs frequently:

- Return JSON wrapped in `​```json … ​```` fences.
- Add a one-line preamble before the JSON.
- Hallucinate fields not in the schema.
- Drop required fields.
- Return tickers in the wrong format (`$NVDA`, `nvda`, `NVDA.US`).

`TickerSuggestionParser` is the defensive layer:

1. Strip code-fence markdown.
2. Tolerate `JsonIgnoreProperties(ignoreUnknown = true)` on every DTO.
3. Normalize tickers: uppercase, strip prefixes, drop suffixes after `.`.
4. Clamp all `*Hint` fields to [0, 1].
5. Reject the entire response if `tickers` is malformed (don't silently drop
   suggestions — log loudly).

A response that fails to parse is recorded with `success = false` in the
audit log; the LLM is skipped for the day.

## 7. Aggregation — consensus over diversity

`LlmTickerAggregator.process(List<LlmResponseEnvelope>)`:

1. For each (symbol, llm) pair, record the LLM's hint scores.
2. Group by symbol.
3. For each symbol:
   - `mentionCount` = number of distinct LLMs that mentioned it
   - Each hint score = weighted mean across mentioning LLMs, using the
     per-LLM trader-set reliability weight
   - `consensusConfidence` = `clamp(mentionCount / providers.size())`
4. Apply the `minConsensusFraction` threshold (configurable, default `0.34`
   = at least one-third of providers).
5. Stamp the result with `singleSource = mentionCount == 1` for review-side
   filtering.

Output is a `List<LlmAggregatedSuggestion>`. The orchestrator converts each
into a `TradeCandidate` with:

```java
candidateId  = "llm-" + symbol + "-" + isoDate(today)
sourceType   = SourceType.LLM_SUGGESTION
sourceId     = comma-separated LLM names that mentioned it
sourceUrl    = primarySourceUrl from the highest-weight mention, if any
catalystType = most-cited catalyst type across mentions
```

The id pattern keeps the day's run idempotent — same ticker mentioned by the
same set of LLMs on the same day produces the same candidate id, so the
existing dedup check in `CandidatePipelineOrchestrator` does its job.

## 8. Daily flow

`LlmTickerScheduler` runs on cron `0 30 11 * * MON-FRI` UTC (~6:30am ET,
before US market open). The flow:

```
1. Build the daily prompt (TickerPromptBuilder)
2. Fan out to all configured LlmProviders in parallel (CompletableFuture)
3. Each provider returns an LlmResponseEnvelope or fails
4. Failures are logged but don't fail the run as long as ≥1 LLM responds
5. LlmTickerAggregator merges responses by symbol with consensus weighting
6. Each aggregated suggestion is converted into a TradeCandidate
7. Candidates flow through CandidatePipelineOrchestrator (market data,
   analytics, validation, persistence) — identical path to SEC and manual
8. By the time the trader opens / at 7am ET, survivors are on the review
   surface alongside the SEC-sourced candidates
```

Total cost per run: typically 3-5 LLM API calls of ~2-3k tokens each. At
current rates, well under $1/day across all providers.

## 9. Configuration

`application.yml`:

```yaml
red-dragon:
  scheduler:
    llm:
      enabled: true
      cron: 0 30 11 * * MON-FRI
      zone: UTC
      max-tickers-per-llm: 5
      min-consensus-fraction: 0.34
      focus-areas: "policy-driven capital flows, sector rotation"
      exclude-watch-list: true     # exclude what's already under review
  llm:
    providers:
      claude:
        enabled: true
        api-key: ${ANTHROPIC_API_KEY:}
        model: claude-sonnet-4-6
        temperature: 0.4
        max-tokens: 4000
        reliability-weight: 1.0
      openai:
        enabled: true
        api-key: ${OPENAI_API_KEY:}
        model: gpt-5
        temperature: 0.4
        max-tokens: 4000
        reliability-weight: 1.0
      gemini:
        enabled: false
        api-key: ${GEMINI_API_KEY:}
        model: gemini-2.0-pro
        temperature: 0.4
        max-tokens: 4000
        reliability-weight: 0.7
```

`reliability-weight` starts at `1.0` for everyone. After a calibration window
(say 90 days), the trader can tune them — an LLM whose suggestions historically
end up with PASS verdicts more often earns a higher weight.

## 10. HTTP surface

| Method | Path                                | Purpose |
| ------ | ----------------------------------- | ------- |
| POST   | `/api/pipeline/llm/run`             | Manual trigger of one LLM run (bypasses the scheduler). |
| GET    | `/api/pipeline/llm/last-response`   | Last raw LLM responses, for audit. |
| GET    | `/api/pipeline/llm/providers`       | List enabled LLM providers and their reliability weights. |
| PUT    | `/api/pipeline/llm/providers/{id}/weight` | Trader-side adjustment of per-LLM reliability weight. |

## 11. Persistence

A new table for audit. Every LLM call gets logged, regardless of whether
the parsed suggestions ended up as candidates.

```sql
-- Flyway V4__llm_audit.sql
create table llm_run (
  id bigint generated by default as identity primary key,
  run_id varchar(64) not null,
  provider varchar(32) not null,
  prompt_template_version varchar(16) not null,
  request_at timestamp not null,
  response_at timestamp,
  duration_ms integer,
  success boolean not null,
  error_message varchar(2048),
  ticker_count integer,
  raw_response text,
  parsed_response text
);

create index idx_llm_run_run_id on llm_run(run_id);
create index idx_llm_run_request_at on llm_run(request_at desc);
```

Raw and parsed responses are kept so the trader can verify the LLM said
what we think it said, and so historical performance per LLM can be
computed for the reliability-weight calibration loop.

## 12. Failure modes

| Failure | Behavior |
| ------- | -------- |
| One LLM API key missing | provider auto-disabled at startup, others run |
| One LLM times out | logged, skipped for the day, others aggregate |
| All LLMs fail | the day produces 0 candidates from this source; SEC + manual still run unaffected |
| LLM returns invalid JSON | logged with raw response, skipped |
| LLM returns 0 tickers | accepted as a valid signal ("nothing today") |
| LLM returns a ticker red-dragon already rejected this week | filtered out by `exclude-watch-list` if enabled |
| LLM hallucinates a non-existent ticker | enrichment fails in `MarketFeatureCalculator` (no bars) → quality `MISSING_SYMBOL` → hard-gated out by validation. No harm done. |

The system is designed so that LLM noise can never cause a false PASS verdict.
The LLM only widens the funnel; everything downstream still applies its own
test.

## 13. Privacy & what leaves the system

The prompt is a generic description of red-dragon's methodology plus the
trader's configurable focus areas. **Nothing about the trader's actual
positions, account, or history is sent to any LLM.**

Each LLM provider sees:
- The system prompt (constant, public framework description)
- The current date
- A list of tickers already under review (used as a negative filter)
- The configured focus-areas string
- A configurable optional macro note

None of: portfolio holdings, realized P&L, the trader's identity, prior
verdicts, or any persistence-layer data.

## 14. Testing

`MockLlmProvider` returns a fixed `LlmResponseEnvelope` so the full pipeline
can be exercised without real API calls. The mock is the bean in `dev` and
`test` profiles by default.

Unit tests cover:
- `TickerSuggestionParser`: code-fence stripping, malformed JSON, missing
  fields, ticker normalization, hint clamping.
- `LlmTickerAggregator`: 1-LLM (single source), 2-LLM consensus, 3-LLM
  consensus, per-provider weight application, threshold filtering.
- `TickerPromptBuilder`: focus-area substitution, exclusion list, template
  versioning.

Integration tests use `MockLlmProvider` to drive a full
`LlmTickerIngestionService.process()` and assert the resulting candidates
reach `CandidateRepository` with the correct `SourceType.LLM_SUGGESTION`.

## 15. Out of scope (explicit)

- **Trading on the LLM's thesis directly.** The LLM nominates; validation
  decides. No code path lets an LLM bypass validation.
- **Per-trade LLM scoring during validation.** Validation is deterministic.
  LLMs can suggest tickers but cannot influence the verdict math.
- **Conversational refinement.** This is a one-shot daily prompt, not a
  back-and-forth. Multi-turn refinement is deferred until single-shot
  precision is proven.
- **Image/document inputs.** Text in, text out. Future expansion may include
  document attachment (e.g. a daily macro brief) but not today.
- **Real-time intraday LLM suggestions.** This is a once-a-day pre-market
  source, not a live feed. Intraday LLM querying would burn cost and produce
  unstable suggestions.

## 16. Open decisions

1. **Should we let the trader veto a ticker before pipeline runs?** Trade-off:
   speed of daily flow vs trader sovereignty. Current default: no veto;
   trader can always reject in review.

2. **Do we trust the LLM's `catalystType` choice, or recompute it?** Current
   plan: trust it, but flag the candidate with a `traderReviewRequired` if
   the LLM and `EightKCategoryMapper` would have disagreed on the same
   underlying filing.

3. **Should consensus weighting use a logarithmic curve?** Currently linear:
   one mention = 1/N, two mentions = 2/N, etc. A diminishing-returns curve
   would dampen the over-confidence of "everyone says NVDA today."

4. **Reliability-weight calibration**: should the system auto-update weights
   from realized verdicts after 90 days, or only allow manual updates?
   Auto-update risks over-fitting to a regime; manual updates risk being
   forgotten.

5. **Cost ceiling enforcement** — if the trader configures 5 providers but
   we want a $20/month cap, should the scheduler skip providers in priority
   order, or fail loudly when the budget runs out? Currently no enforcement;
   first MVP is "trust your provider quotas."

## 17. Notes for future-Claude

When implementing this, the high-risk areas are:

- **Prompt drift**: every prompt change should bump
  `prompt-template-version` so the audit log reflects which prompt produced
  which suggestion.
- **JSON parsing brittleness**: the parser is the most failure-prone piece.
  Tests should include real-world malformed outputs (fenced JSON, preamble
  text, hallucinated fields, mixed types).
- **Provider parallelism**: use `CompletableFuture` with a bounded thread
  pool so a slow LLM can't hold up the whole run. Set per-provider timeout
  at ~30s.
- **Idempotency**: the `candidateId` pattern (`llm-{symbol}-{isoDate}`)
  combined with same-day re-runs means the orchestrator dedup check handles
  the "scheduler accidentally fired twice" case automatically.
- **Don't store the trader's API keys in the audit log**, even if a 500 from
  the LLM provider includes them in an error message. Scrub headers before
  persisting.
