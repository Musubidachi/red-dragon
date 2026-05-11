# lib-validation — Validation Framework

Architecture doc that captures the user's validation philosophy and translates
it into the engineering surface of `lib-validation`. Companion to the existing
`package-info.java` (which describes the two-stage hard-rules + score-aggregation
pipeline) and to the `SecCandidate` / `OrderRequest` contracts that bound the
module on both sides.

> **Status**: design only. No validation code has been written yet. This doc is
> the input to the implementation pass.

---

## 1. Core philosophy

The validation framework is **disequilibrium-centric, not prediction-centric.**
It does not ask "will price go up?" It asks one question, repeatedly, across
multiple lenses:

> **Has reality changed before equilibrium fully adapts?**

A candidate passes only when the framework can answer "yes" to that question
with sufficient confidence. What the framework is **not** validating:

- Whether people are bullish on the ticker.
- Whether the stock is "good."
- Whether momentum exists in isolation.

What it **is** validating:

- Whether equilibrium has shifted (something real changed).
- Whether the market has not fully adapted (we are early).
- Whether the probability distribution remains asymmetric in our favor.

This stance shapes every concrete decision in the rest of the doc. When a
proposed feature would push the system toward generic bullishness-detection or
sentiment-chasing, push back to first principles: real, early, asymmetric.

---

## 2. The seven validation dimensions

The framework decomposes into seven dimensions. Each dimension has a question
it answers and a engineering shape (hard rule, score, or hybrid). The
dimensions are evaluated in a fixed order (§4) so the cheapest, most
deterministic checks fail fast before expensive scoring runs.

| # | Dimension              | Question                                                    | Shape                         |
| - | ---------------------- | ----------------------------------------------------------- | ----------------------------- |
| 1 | Structural Reality     | Did something actually change?                              | Hard rule + score             |
| 2 | Material Significance  | Is it meaningful relative to company size and capital flow? | Score                         |
| 3 | Earlyness              | Am I early enough in propagation?                           | Score (partly manual)         |
| 4 | Equilibrium Quality    | Is restoration / expansion behavior intact?                 | Score (regime-derived)        |
| 5 | Reflexivity Potential  | Will propagation amplify the move?                          | Score (partly manual)         |
| 6 | Asymmetry              | Is the payoff still favorable?                              | Score (from `lib-analytics`)  |
| 7 | Regime Compatibility   | Does the environment support the strategy?                  | Hard rule + score modifier    |

A separate **Deployment Confidence** layer aggregates these into a final
PASS / WATCH / REJECT verdict with a confidence tier (§8).

---

## 3. Dimension-by-dimension translation

### 3.1 Structural Reality — "Did something actually change?"

The strongest filter. Naturally skeptical of hype, sentiment-only narratives,
and emotional crowd interpretation. Validates factual catalysts, structural
implications, and capital-flow significance.

Examples of real catalysts: government grants, policy changes, supply
constraints, contracts, sector incentives, liquidity shifts, structural demand
changes.

**Engineering translation:**

*Hard rule (source credibility):*

- Candidate's `source` must be from an enumerated allowlist. From the existing
  ingestion design that means: SEC filing (8-K, Form 4, 13D/G, S-1/S-3/424B),
  press release from the issuer, government/regulatory source, or major news
  desk with a named author. Reject anonymous social or unverified rumor at
  the gate. Reflexivity (§3.5) is a *separate* dimension, so this filter does
  not punish a high-mention-velocity story whose underlying catalyst is real.
- For 8-K candidates: the Item code must map to a non-trivial category from
  the SEC taxonomy. Item 9.01 ("financial statements and exhibits") alone is
  not a catalyst — it must accompany a content item.

*Score (Catalyst Reality Score, 0–100):*

- `source_credibility`: 0–100, derived from source type.
- `catalyst_specificity`: was the event concrete and quantifiable (a contract
  with named counterparty, a grant with dollar amount) vs vague? 0–100.
- `capital_flow_implication`: does the event change the issuer's future cash
  flows, market structure, or supply/demand? Binary or 0–100 with explanation.

### 3.2 Material Significance — "Is it materially meaningful?"

Materiality is relative. A $10M contract for a $200M small-cap is meaningful;
a $10M contract for AAPL is noise.

**Engineering translation:**

*Score (Materiality Score, 0–100):*

- For dollar-amount events: ratio of event size to issuer's annual revenue
  and/or market cap. Bucketed (e.g., > 10% mcap → 100; 1–10% → 60; < 1% → 20).
- For insider buys: dollar amount + relationship + cluster context. Multiple
  insiders buying in a short window scores far higher than a single token
  director purchase.
- For 13D/G filings: percent of class acquired and filer type (known activist
  → bonus weight).
- For policy/macro events: sector concentration. A semiconductor export
  control change is high-material for $SOXX constituents and irrelevant for
  utilities.

Materiality is the gate that separates "real but trivial" from "real and
worth acting on."

### 3.3 Earlyness — "Am I early enough in propagation?"

Ideal preferences:

| Narrative Stage           | Preference    |
| ------------------------- | ------------- |
| Unknown but real          | VERY HIGH     |
| Emerging propagation      | HIGH          |
| Broad social acceleration | CAUTION       |
| Mainstream saturation     | LOW           |
| Euphoric reflexivity      | EXIT-ORIENTED |

**Engineering translation:**

*Score (Earlyness Score, 0–100):*

- `time_since_first_observable`: hours since the catalyst was first
  surfaceable in our ingestion. SEC filings expose this cleanly (`filedAt`).
- `prior_coverage_count`: count of news articles, RSS items, or mentions
  preceding our observation. Lower is better.
- `price_response_so_far`: % move since the catalyst date, normalized by ATR.
  A 5x-ATR move already happened means we're not early.
- *Manual factor*: a `propagation_stage` enum (`unknown`, `emerging`,
  `accelerating`, `mainstream`, `euphoric`) that the user can override at
  decision time. The system proposes a stage; the user confirms or corrects.

This is the dimension most dependent on data we don't yet ingest (news/RSS,
social mention tracking). For the v1 SEC-only pipeline, earlyness collapses
toward `time_since_first_observable + price_response_so_far`. That's a
partial signal and the doc should be honest about that.

### 3.4 Equilibrium Quality — "Is restoration / expansion behavior intact?"

This dimension comes directly from the user's short-frequency trading roots.
The system needs to detect environments where equilibrium assumptions remain
valid versus environments where they have broken down.

**Engineering translation:**

This dimension is largely **derived from `regime_snapshot`** rather than
computed inside `lib-validation`. The work has already been delegated to
`lib-analytics`. What `lib-validation` does is *consume* the regime label
and apply thesis-specific compatibility logic.

*Score (Equilibrium Quality Score, 0–100):*

- For mean-reversion-type theses: high in `rotational_equilibrium`, neutral
  in `volatility_compression`, low in `directional_expansion`, very low in
  `news_driven_shock` / `macro_instability`.
- For trend-following / breakout theses: inverted — high in
  `directional_expansion`, low in `rotational_equilibrium`.
- For premium-collection (covered call) theses: high when realized vol is
  compressed and the underlying thesis is intact.

The thesis ↔ regime compatibility matrix is one of the artifacts this doc
produces. It must be made explicit, not buried inside scoring code.

### 3.5 Reflexivity Potential — "Will propagation amplify the move?"

Reflexivity is **not** sentiment chasing. It is the recognition that:

- narratives influence participation,
- participation influences price,
- price influences narrative.

We are validating whether structural reality is likely to become socially
amplified, not whether social positivity is currently high.

**Engineering translation:**

*Score (Reflexivity Score, 0–100):*

- `mention_velocity`: rate of change of mentions in the last 24h. Needs
  news/RSS or social ingestion we don't have yet — defer or proxy.
- `sector_clustering`: are adjacent tickers in the same sector moving in
  sympathy? This we *can* compute from market data alone (correlations,
  same-sector RVOL spike).
- `volume_acceleration`: is volume on this ticker accelerating relative to
  20-day average? Computable from market data.
- `narrative_coherence`: is there a single dominant explanation, or
  scattered fragments? Manual factor for now.

**Hard distinction (state it loudly):** raw bullishness, emotional hype, and
social positivity are *not* primary inputs. The framework evaluates
propagation dynamics, not crowd mood.

### 3.6 Asymmetry — "Is the payoff still favorable?"

The framework consistently seeks asymmetric disequilibrium: limited downside
relative to potential equilibrium shift. Highly sensitive to *compression*
of asymmetry — i.e., the move has already largely happened.

**Engineering translation:**

*Score (Asymmetry Score, 0–100):* — produced upstream by `lib-analytics`'s
asymmetry scorer. `lib-validation` consumes it and combines it with the
saturation gate below.

*Hard rule (saturation gate):* reject when

- the price has moved more than `N` ATRs in the last `M` days (configurable),
  AND
- the catalyst-implied repricing is judged largely complete.

Reasons to reject under this gate, copied from the source framework:

- the move already expanded too far,
- volatility has become unstable,
- equilibrium has already repriced,
- narrative saturation is excessive,
- participation has become euphoric.

The saturation gate is the framework's defense against late entries on
real catalysts. It is the dimension that turns "I was right about the
catalyst" into "I was right but too late to deploy."

### 3.7 Regime Compatibility — "Does the environment support the strategy?"

The most subconscious part of the user's process when discretionary, and the
most important to encode explicitly when automated. The user's edge depends
on market-state compatibility; participation should compress when the
governing regime is hostile.

**Engineering translation:**

*Hard rule (regime gates):*

- If regime is `macro_instability` or `news_driven_shock`: REJECT or downgrade
  PASS → WATCH across the board. The system fails closed when the
  environment itself is fragile.
- If regime is `liquidity_deterioration`: REJECT for thesis types that need
  smooth fills (mean-reversion). Allow PASS for premium-collection only on
  positions already held.

*Score modifier (Regime Bonus, −20 to +20):*

- Applied to the aggregated confidence score after all other dimensions.
- Positive when the regime amplifies the thesis (rotational + mean-reversion,
  trending + breakout).
- Negative when the regime fights it.

### 3.8 Deployment Confidence — meta-aggregation

This is the framework's final answer, not a separate dimension. The other
seven feed into it. The user historically used whole-account concentration,
selective aggression, and deliberate inactivity between high-conviction
deployments. The system's job is to **only surface candidates that meet the
threshold for selective deployment**, not constant participation.

Mapped to existing `VerdictTier`:

- `PASS_CONCENTRATED` — high confidence across all relevant dimensions. The
  type of candidate that historically justified concentration.
- `PASS_PROBE` — solid candidate but not concentration-worthy. Suitable for
  smaller deployment.
- `WATCH` — interesting but missing one or more dimensions. Not actionable
  yet; surface in the watchlist view.
- `REJECT` — fails a hard rule or scores below threshold on a critical
  dimension.

---

## 4. Validation hierarchy (evaluation order)

Validation runs as a short-circuit chain. Hard rules first; expensive scoring
last; manual factors injected at the end.

```
1. Hard rule: source credibility (Structural Reality 3.1)
2. Hard rule: account / instrument compatibility
3. Hard rule: regime hostility gate (Regime 3.7)
4. Hard rule: saturation gate (Asymmetry 3.6)
5. Hard rule: liquidity floor
   ─── short-circuit on any reject above ───
6. Score: Catalyst Reality (3.1)
7. Score: Materiality (3.2)
8. Score: Earlyness (3.3)
9. Score: Equilibrium Quality (3.4)
10. Score: Reflexivity Potential (3.5)
11. Score: Asymmetry (3.6)
12. Apply Regime Bonus modifier (3.7)
13. Aggregate → Deployment Confidence (3.8)
14. Emit ValidationVerdict
```

Steps 1–5 are cheap and deterministic. Steps 6–11 may require fetching market
data or computing features and should be done lazily / cached. Steps 12–14
are pure aggregation.

---

## 5. Data inputs needed (and gaps today)

| Dimension              | Required inputs                                                 | Available today?           |
| ---------------------- | --------------------------------------------------------------- | -------------------------- |
| Structural Reality     | `Candidate.source`, `Candidate.payload` (SEC item codes etc.)   | ✅ from `lib-ingestion`    |
| Material Significance  | Issuer revenue/mcap, dollar amount in catalyst                  | Partial — needs fundamentals lookup |
| Earlyness              | Filing time vs first-observable, prior coverage count, price-since-catalyst | Partial — coverage count requires news ingestion |
| Equilibrium Quality    | `regime_snapshot` label and confidence                          | ⏳ needs `lib-analytics` regime classifier |
| Reflexivity Potential  | Volume acceleration, sector RVOL, mention velocity              | Partial — mentions need news/social |
| Asymmetry              | `lib-analytics` asymmetry score, price-vs-ATR over recent days  | ⏳ needs `lib-analytics`   |
| Regime Compatibility   | `regime_snapshot` label                                         | ⏳ needs `lib-analytics`   |

A reasonable first deliverable: validate on **Structural Reality + Materiality
+ Saturation + Regime hostility** alone, using only ingestion + a rule-based
regime classifier. That covers the highest-leverage filters with the
shallowest dependency surface.

---

## 6. What we can and cannot automate today

**Fully automatable now:**

- Source credibility hard rule (allowlist of source types from
  `lib-ingestion`).
- Saturation gate using ATR + days-since-catalyst.
- Regime hostility hard rule (once `regime_snapshot` exists).
- Materiality scoring against company fundamentals (once we have a
  fundamentals lookup, even a daily-cached one).
- Asymmetry score consumption (once `lib-analytics` produces it).

**Partially automatable — needs proxy or manual factor:**

- Earlyness — automatable in the SEC-only data shape; less so for non-filing
  ideas until news/RSS ingestion lands. Default to *manual factor* with a
  system-suggested stage.
- Reflexivity — automatable via volume / sector-correlation proxies. The
  mention-velocity component is deferred until news/social ingestion exists.

**Irreducibly discretionary (surface, don't replace):**

- Final deployment-confidence call when the system says WATCH and the user
  thinks PASS, or vice versa. The system records its verdict and the user's
  override; both are written to `validation_verdict` for later analysis.
- Narrative coherence judgment.

The principle from earlier sessions still holds: the system augments the
trader's discretion. It does not replace it. Manual factors are first-class
inputs, captured on the verdict so reasoning is auditable.

---

## 7. `ValidationVerdict` shape

The contract `lib-validation` emits. Java 21 records, consumed by `app` for
the review surface and by `lib-persistence` for storage.

```java
package dev.reddragon.validation;

record ValidationVerdict(
    UUID validationVerdictId,
    UUID candidateId,
    Instant evaluatedAt,
    VerdictTier tier,                       // PASS_CONCENTRATED | PASS_PROBE | WATCH | REJECT
    int confidenceScore,                    // 0–100 aggregate
    List<HardRuleOutcome> hardRules,
    List<DimensionScore> dimensionScores,
    int regimeBonus,                        // −20 to +20
    List<ManualFactor> manualFactors,       // user-provided at decision time, if any
    String reasoningSummary                 // human-readable summary auto-generated from above
) {}

enum VerdictTier { PASS_CONCENTRATED, PASS_PROBE, WATCH, REJECT }

record HardRuleOutcome(
    String ruleId,                          // 'source_credibility', 'saturation_gate', etc.
    boolean passed,
    String explanation                      // why it passed/failed
) {}

record DimensionScore(
    Dimension dimension,                    // STRUCTURAL_REALITY, MATERIALITY, EARLYNESS, ...
    int score,                              // 0–100
    double weight,                          // contribution to aggregate
    List<Factor> factors                    // sub-factors with their values
) {}

enum Dimension {
    STRUCTURAL_REALITY,
    MATERIALITY,
    EARLYNESS,
    EQUILIBRIUM_QUALITY,
    REFLEXIVITY,
    ASYMMETRY,
    REGIME_COMPATIBILITY
}

record Factor(String name, double value, double weight, String note) {}

record ManualFactor(String name, String value, String enteredBy, Instant enteredAt) {}
```

Every verdict carries its full reasoning chain: which hard rules ran and
their outcome, every dimension score with its sub-factors, the regime bonus
applied, any manual factors the user injected, and a human-readable
auto-generated summary. The summary is convenience; the structured fields are
truth.

---

## 8. Scoring rubric (starter weights)

Initial dimension weights. These are starting points to be tuned with data.
The doc commits to a **transparent weighted sum** — no black-box ML, no
opaque transforms. Every change to weights should be a tracked decision.

| Dimension              | Weight   | Notes                                         |
| ---------------------- | -------- | --------------------------------------------- |
| Structural Reality     | 0.20     | Foundation; low score here makes others moot. |
| Material Significance  | 0.15     | Filters "real but trivial."                   |
| Earlyness              | 0.20     | Highest user-stated emphasis.                 |
| Equilibrium Quality    | 0.10     | Heavier for mean-reversion theses.            |
| Reflexivity Potential  | 0.10     | Lower until news/social ingestion lands.      |
| Asymmetry              | 0.20     | Co-primary with Earlyness.                    |
| Regime Compatibility   | 0.05 + bonus | Most expressed via the bonus modifier.    |

Aggregate score (0–100) → tier mapping:

| Aggregate | Tier                |
| --------- | ------------------- |
| ≥ 80      | PASS_CONCENTRATED   |
| 60 – 79   | PASS_PROBE          |
| 40 – 59   | WATCH               |
| < 40      | REJECT              |

Any hard-rule failure short-circuits to REJECT regardless of the aggregate.

These thresholds are deliberately conservative. The framework's stated bias
is selective aggression — the system should err on the side of WATCH rather
than PASS until evidence accumulates that PASS_PROBE is too restrictive.

---

## 9. Hard rules catalog (v1)

Explicit list of binary gates. Each rule has an ID, a one-line statement, and
the data it requires. Implementation lives in `dev.reddragon.validation.rules`.

| ID                          | Rule                                                              | Inputs                                      |
| --------------------------- | ----------------------------------------------------------------- | ------------------------------------------- |
| `source_credibility`        | Source must be on the allowlist (SEC, named press, gov, named author). | `Candidate.source`, `Candidate.sourceMeta` |
| `account_instrument_allowed` | Instrument type permitted in target account (no shorts in IRA).  | `Account.accountType`, `Candidate.instrumentType` |
| `regime_hostility`          | Reject if regime ∈ {macro_instability, news_driven_shock}.         | `regime_snapshot.label`                     |
| `saturation_gate`           | Reject if price has moved > N ATRs in M days post-catalyst.        | `marketData.atr`, `Candidate.filedAt`, price series |
| `liquidity_floor`           | Reject if 20-day avg dollar volume < threshold.                    | `marketData.dollarVolume20d`                |
| `options_chain_exists`      | If candidate intent is option trade, chain must exist for ticker.  | `marketData.optionChain(ticker)`            |
| `not_in_blocklist`          | Ticker not on a personal blocklist (configurable).                 | config `validation.blocklist[]`             |
| `8k_item_not_trivial`       | For 8-K candidates: items must include at least one non-9.01.      | `Candidate.payload.itemCodes`               |

Configurable thresholds (e.g., `saturation_gate.maxAtrMoves`,
`liquidity_floor.minDollarVolume`) live in `application.yml` under
`validation.hardRules.*`. No magic numbers in code.

---

## 10. PASS / WATCH / REJECT semantics

Concrete meaning the rest of the platform commits to:

- **PASS_CONCENTRATED** — Surface in the active queue with a strong-signal
  badge. Pre-fill a `trade_decision` draft. User confirms or rejects;
  validator does not place the order itself.
- **PASS_PROBE** — Surface in the active queue. Pre-fill a smaller-sized
  `trade_decision` draft. Same user-confirmation step.
- **WATCH** — Surface in the watchlist view. No `trade_decision` draft.
  Re-evaluated automatically when upstream state changes (new filing on the
  same ticker, regime flip, asymmetry score change).
- **REJECT** — Logged and counted; not surfaced in any active or watchlist
  view. Hard-rule failures are logged with the specific rule ID for diagnostics.

User overrides are first-class: the user can re-rank a WATCH to PASS_PROBE
manually, and the system records the override on the `validation_verdict` row
(`overridden_tier`, `override_reason`). Over time these overrides become the
data for tuning weights — the system learns from the user, not from
backtests.

---

## 11. Out of scope (explicit)

- **Position sizing.** Sizing belongs to a future `lib-risk` or sits inside
  `trade_decision` defaults. The validator outputs a tier; size is a separate
  call.
- **Order construction.** That's `lib-execution`'s job. The validator does
  not know about Schwab order shapes.
- **Trade timing within the day.** The validator outputs PASS/WATCH/REJECT,
  not "buy at 10:32." Intraday execution timing is the user's call.
- **Backtesting / historical replay.** The framework is forward-only. A
  separate evaluation harness can replay verdicts against later price action
  to measure performance, but it lives outside this module.
- **Machine learning.** Transparent weighted sums only. ML can be revisited
  once the deterministic version has produced enough verdicts to learn from,
  but that's not a v1 concern.

---

## 12. Open decisions

1. **Manual-factor capture surface.** Where does the user enter
   `propagation_stage` or `narrative_coherence`? In the review UI, in the
   verdict-confirmation step, or asynchronously?
2. **Thesis-type-aware scoring.** Equilibrium Quality has different weights
   for mean-reversion vs trend vs premium-collection. Should the validator
   take a `thesisHint` from the candidate (or from a separate decision-side
   input), or run all three scorers and pick the highest?
3. **Materiality without fundamentals.** Before we have a fundamentals data
   source wired up, what's the materiality fallback? Reasonable default:
   bucket by mcap from `company_tickers_exchange.json` (which gives exchange
   listing) plus a manual override.
4. **Override-driven weight tuning.** When does the system start using user
   overrides as evidence to re-weight? Open: need a minimum sample size and
   a way to detect non-stationarity.
5. **Hard-rule short-circuit logging.** When a candidate is rejected at
   step 1, do we still compute and log the downstream scores for diagnostic
   value? Recommend: yes, but only when a debug flag is enabled.
6. **Regime-dependent saturation thresholds.** A 5-ATR move in a
   `volatility_compression` regime is more saturating than the same move in
   `directional_expansion`. Worth making `saturation_gate.maxAtrMoves`
   regime-aware? Defer to v2.

---

## 13. Suggested implementation order

1. **`ValidationVerdict` and supporting record types** in
   `dev.reddragon.validation`. SPI only; no logic.
2. **Hard-rule engine** — interface + a `RuleSet` orchestrator that runs
   rules in order and short-circuits on REJECT. Wire the eight rules from §9
   as no-op stubs.
3. **`source_credibility` and `not_in_blocklist`** — simplest rules, no
   external data. Get the plumbing right before adding data-dependent rules.
4. **`account_instrument_allowed`** — needs the `Account` type. Easy once
   that exists.
5. **`8k_item_not_trivial`** — pure function over the `SecCandidate` payload.
6. **Liquidity floor** — needs `lib-marketdata` providing 20-day average
   dollar volume.
7. **Saturation gate** — needs market data + ATR computation.
8. **Regime hostility rule + Regime Bonus modifier** — needs
   `regime_snapshot` from `lib-analytics`.
9. **Dimension scorers** in the order weighted highest first: Structural
   Reality → Asymmetry → Earlyness → Materiality → Equilibrium Quality →
   Reflexivity → Regime Compatibility.
10. **Aggregator** — weighted sum, regime bonus application, tier mapping.
11. **Reasoning summary generator** — string templating from the structured
    score breakdown. Pure function over the verdict.
12. **Override capture** — `overridden_tier` and `override_reason` fields
    plumbed end-to-end.

Each step is independently testable. Steps 1–5 produce a system that emits
non-trivial REJECTs and PASS-without-scoring. Steps 6–8 fill in the
deterministic gates. Steps 9–11 turn it into the full pipeline.

---

## 14. Notes for future-Claude

- This framework was authored from a user-supplied "Validation Pattern
  Summary" on 2026-05-10 that captured the user's discretionary process via
  GPT introspection. It is the user's authoritative philosophy statement.
  When implementation forces choices that conflict with the philosophy,
  surface the conflict to the user — don't quietly resolve it in code.
- **Disequilibrium-centric, not prediction-centric.** Any feature framed as
  "predict X" is a smell. The framework asks "has reality shifted before
  equilibrium adapts?", not "what will price do next?"
- **No black-box ML in v1.** Transparent weighted sums and explicit rule
  outcomes. Auditability of *why* a verdict landed is non-negotiable.
- **Manual factors are first-class.** The framework explicitly preserves
  user discretion on dimensions that resist automation (propagation stage,
  narrative coherence). Don't try to fully automate them — capture them as
  inputs the user injects at decision time.
- **Selective aggression > constant participation.** The framework's bias is
  to surface fewer, higher-quality candidates. When tuning thresholds,
  default to stricter rather than looser.
- **The validator does not place orders, size positions, or time intraday
  entries.** It outputs PASS / WATCH / REJECT with reasoning. Everything
  downstream of that is a different module.
