# lib-analytics — Meta-Adaptation Feedback Loops

A design doc for how red-dragon learns from its own decisions as markets
change. This is the long-term expansion of MD Layer 8 (Meta-System
Adaptation), past the basic drift detection that already ships.

The intent is *not* to build an auto-tuning trading system. It is to build a
system that surfaces, with statistical discipline, when the trader's
framework or thresholds may need revision — and lets the trader stay in
the loop for every change.

This is not yet implemented. It lives next to `LLM_TICKER_SCHEDULER.md` so
the design is reviewable before code lands.

## 1. Why this exists

Markets change in ways that quietly erode the edge a system was built on:

- A catalyst type that used to predict moves gets arbitraged out of the data
- A regime that supported mean-reversion shifts to one that punishes it
- Propagation speeds up so "earlyness" is over by the time the system labels it
- The trader's own override patterns reveal a structural blind spot

A drift detector that only reports "win rate is down 8%" tells you *that*
something changed, but not *what* or *where*. The aim of this doc is a
feedback architecture that names the change concretely enough to act on,
without auto-correcting itself into the past.

## 2. What ships today

The current state of L8 is small and worth keeping in view:

- `LongHorizonCalibrationAnalyzer` computes a single `CalibrationDriftLevel`
  (STABLE / MINOR_DRIFT / MODERATE_DRIFT / MAJOR_DRIFT) from rolling win
  rate, average return, and average drawdown across all `OutcomeSample`s.
- `CalibrationOutcomeService` persists outcomes via `CalibrationOutcomeEntity`
  (Flyway V2) and aggregates them on demand.
- The trader can submit outcomes through `POST /api/calibration` and read
  the current report through `GET /api/calibration`.

This is the foundation. Everything in this doc layers on top of it.

## 3. Three loops, three time scales

Different feedback loops answer different questions at different cadences.
A single drift number can't serve all three.

### 3.1 Short loop — per-trade, attribution

**Question:** Did each scoring dimension actually predict what happened?

Today `OutcomeSample` stores all eight score dimensions at decision time
plus a binary `thesisWorked`. That data is sufficient for per-dimension
correlation analysis. Add a new analyzer:

```
DimensionAttributionAnalyzer.process(List<OutcomeSample>) -> AttributionReport
```

The output names, per dimension, the Pearson correlation between the
dimension's score at decision time and the realized return. Over a rolling
window of N samples, you can see which dimensions are pulling weight and
which have decayed to noise.

What it answers in practice:

- "Structural reality still correlates 0.62 with outcome — keep weighting it"
- "Reflexivity potential has decayed to 0.04 correlation — your reflexivity
  scoring is no longer measuring what you think it measures"
- "Earlyness has gone *negative* (-0.12) — you're systematically labeling
  late-stage moves as early. Time to re-examine the heuristic."

A negative-correlation dimension is the loudest signal in the system —
it means the score is pushing decisions the wrong way and should be
investigated before the next deployment cycle.

### 3.2 Medium loop — per-stratum, calibration

**Question:** Where is my edge actually living?

The current calibration lumps every trade together. Stratify it. Group
`OutcomeSample` by `regimeLabel`, by `catalystType`, and by `deploymentTier`,
and compute drift independently within each stratum.

```
StratifiedCalibrationAnalyzer.process(List<OutcomeSample>) ->
        Map<Stratum, CalibrationReport>
```

What it answers in practice:

- "Framework is STABLE overall, but MAJOR_DRIFT in `GOVERNMENT_GRANT`
  catalysts during `SUPPORTIVE_TREND` regimes" — your grant signal is
  getting front-run when trends are persistent
- "STABLE in `STANDARD` tier, MODERATE_DRIFT in `CONCENTRATED` tier" —
  the high-conviction picks are over-confident; pull the concentration
  threshold up
- "Tech sector PASS rate is 0.42; biotech PASS rate is 0.71" — a sector
  drift detector emerges as a side benefit

Minimum stratum size: 50 trades (configurable). Below that, the analyzer
should explicitly report "insufficient samples" rather than emit
low-confidence noise.

### 3.3 Long loop — framework-level, monthly

**Question:** Is the framework itself stale?

The MD's L8 raises questions that no per-trade analysis can answer:

- *Are equilibrium assumptions weakening?*
- *Is propagation occurring faster?*
- *Is edge persistence degrading?*
- *Are asymmetry windows compressing?*

These need *market-structure* observations, not just trade outcomes. Add
a `MarketStructureDriftMonitor` that runs nightly against the
`market_bar` and `market_snapshot` tables and computes:

| Metric | What it tells you |
|---|---|
| **VWAP reclaim → reversion rate** (rolling 90-day) | When the system says "VWAP reclaim is constructive," does price still revert 7 days later as often as it did a year ago? If not, the rotational assumption is weakening. |
| **Asymmetry half-life** | From the moment a candidate enters with asymmetry ≥ 0.75, how many days until its range position crosses 0.85? Shortening = propagation faster, earlyness threshold needs to fall. |
| **Catalyst-to-repricing lag** | From the SEC filing timestamp to the price move that consumes the asymmetric edge — measured in hours. Compress over time = market is pricing in filings faster. |
| **Regime persistence** | How long does a `SUPPORTIVE_ROTATIONAL` label hold once entered? Shortening = regime classification is unstable. |

These are quarterly review numbers, not weekly. They're tracked on the
review dashboard with year-ago comparisons and trend arrows. When any of
them moves by more than ~25% from the prior-year baseline, the dashboard
flags it for trader review with a one-line interpretation.

When *framework*-level metrics drift, the answer is rarely "tune a
threshold." It's "the model of how markets work needs a structural
revision." That revision is the trader's job; the system's job is to
make the question visible.

## 4. Two often-missed signals

### 4.1 The override loop

The single highest-signal data the system can collect is the trader's own
disagreement with it. When you override a verdict:

- REJECT → PASS: the system was too strict
- PASS → REJECT: the system was too loose
- WATCH → IGNORE: the system was too noisy

Add:

```
trader_override (
  id, candidate_id, original_verdict, override_verdict,
  reason_note, overridden_at
)
```

After 50 overrides, the analyzer asks: did your overrides outperform the
system's verdicts on those candidates? If they did, the system is missing
something your judgment catches.

To go further: vectorize your `reason_note` text via the LLM scheduler's
already-wired LLM provider (separate prompt) and cluster overrides by
theme. The themes that recur become candidate inputs for new scorer
dimensions. The system slowly learns the structure of the trader's tacit
knowledge — but only through the trader naming it.

### 4.2 The counterfactual loop

The system is judged on PASS outcomes. REJECTs are invisible. That bias
leaves *false negatives* uncalibrated forever.

Add a shadow track:

1. Every REJECT verdict is stored with the candidate's symbol and date
2. The backtest engine replays the next 14 days of bars for the rejected
   symbol
3. Compute the realized return *as if* the trade had been taken
4. Aggregate: "of last quarter's REJECTs, X% had realized return > 5%
   that the trader missed"

If the REJECT set's outperformance approaches or exceeds the PASS set's,
the gates are too tight. The trader is being protected from gains the
system doesn't see as gains.

This is computationally cheap because the backtest engine already exists
and the rejected candidates are already persisted. The new piece is a
nightly job that replays them and reports the result.

## 5. The proposed-not-enforced contract

The single most important property of this whole subsystem:

> No threshold or weight change takes effect without the trader's explicit
> approval. The system proposes. The trader disposes.

Every proposed change surfaces on the review dashboard with:

- The exact threshold and its current value
- The proposed new value
- The 95% confidence interval on the change
- The sample size that produced the proposal
- The stratum (regime / catalyst / tier) the change applies to
- The expected impact: what would have changed on the last 30 candidates
- A one-click accept / reject / defer

Every accept is logged with the trader's reason text in a
`threshold_change_log` table. That changelog becomes its own input to
future calibration — when a change is accepted then reversed, the
analyzer notices and stops proposing similar changes for that stratum.

This contract is the firewall that keeps the system from over-fitting
to last week's regime.

## 6. Data model changes

To support the loops above, three new persistence concepts are needed.
None of them are large. All of them should be added now, even before the
analyzers exist, so a year of stratified history is sitting there when
the analyzers come online.

### 6.1 Outcome enrichment

Extend `CalibrationOutcomeEntity` with:

```sql
-- Flyway V5__outcome_stratification.sql
alter table calibration_outcome
  add column regime_label varchar(64),       -- captured at decision time
  add column catalyst_type varchar(64),
  add column deployment_tier varchar(32),
  add column sector varchar(64);

create index idx_outcome_regime         on calibration_outcome(regime_label);
create index idx_outcome_catalyst_type  on calibration_outcome(catalyst_type);
create index idx_outcome_tier           on calibration_outcome(deployment_tier);
```

All four columns come from data already on the candidate at decision time;
they just need to be captured into the outcome row when it's persisted.

### 6.2 Trader overrides

```sql
-- Flyway V6__trader_override.sql
create table trader_override (
  id bigint generated by default as identity primary key,
  candidate_id varchar(128) not null,
  symbol varchar(16) not null,
  original_verdict varchar(32) not null,
  override_verdict varchar(32) not null,
  reason_note varchar(2000),
  overridden_at timestamp not null
);

create index idx_trader_override_candidate on trader_override(candidate_id);
create index idx_trader_override_time      on trader_override(overridden_at desc);
```

### 6.3 Threshold change log

```sql
-- Flyway V7__threshold_change_log.sql
create table threshold_change_log (
  id bigint generated by default as identity primary key,
  threshold_name varchar(128) not null,
  stratum varchar(128),               -- nullable for global changes
  old_value double precision not null,
  new_value double precision not null,
  confidence_interval_lower double precision,
  confidence_interval_upper double precision,
  sample_size integer not null,
  proposed_at timestamp not null,
  decided_at timestamp,
  decision varchar(16) not null,      -- ACCEPTED / REJECTED / DEFERRED / REVERTED
  trader_reason varchar(2000)
);

create index idx_threshold_change_threshold on threshold_change_log(threshold_name, proposed_at desc);
```

### 6.4 Market-structure observations

For the long loop. Computed nightly, stored as a time series so trend
arrows on the dashboard have something to read.

```sql
-- Flyway V8__market_structure_observation.sql
create table market_structure_observation (
  id bigint generated by default as identity primary key,
  observed_at date not null,
  metric_name varchar(64) not null,           -- e.g. "vwap_reclaim_reversion_rate_90d"
  metric_value double precision not null,
  sample_size integer not null
);

create index idx_market_structure_metric_time
  on market_structure_observation(metric_name, observed_at desc);
```

## 7. Where the code lives

```
lib-analytics/src/main/java/dev/reddragon/analytics/services/meta/
    LongHorizonCalibrationAnalyzer.java     # exists today
    LiveContextAdaptationAnalyzer.java      # exists today
    DimensionAttributionAnalyzer.java       # NEW — short loop
    StratifiedCalibrationAnalyzer.java      # NEW — medium loop
    MarketStructureDriftMonitor.java        # NEW — long loop
    TraderOverrideAnalyzer.java             # NEW — override loop
    CounterfactualReplayAnalyzer.java       # NEW — counterfactual loop
    ThresholdChangeProposer.java            # NEW — proposed-not-enforced producer

lib-domain/src/main/java/dev/reddragon/domain/models/
    AttributionReport.java                  # NEW — per-dimension correlations
    StratifiedCalibrationReport.java        # NEW — drift per stratum
    MarketStructureSnapshot.java            # NEW — long-loop output
    ThresholdChangeProposal.java            # NEW — single proposed change
    TraderOverride.java                     # NEW — override domain object
```

Persistence concerns (override repository, threshold-change repository,
market-structure observation repository) live in lib-persistence under
the existing repositories sub-package.

Scheduling (nightly counterfactual replay, nightly market-structure
observation) lives in `app/services/pipeline/` alongside the existing
`SecWatchListScheduler`.

## 8. Trigger cadence

| Loop | Cadence | Where it runs |
|---|---|---|
| Short — dimension attribution | On every outcome ingest | `CalibrationOutcomeService.analyzeAndAppend` |
| Medium — stratified calibration | On every outcome ingest | Same |
| Long — market structure | Nightly cron `0 0 5 * * *` UTC | `MarketStructureDriftScheduler` (new) |
| Counterfactual replay | Nightly cron `0 30 5 * * *` UTC | `CounterfactualReplayScheduler` (new) |
| Override analysis | On every new override row | `TraderOverrideListener` (new) |
| Threshold-change proposals | After medium-loop run, if drift changed | `ThresholdChangeProposer` |

## 9. HTTP surface

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/meta/attribution`               | Per-dimension correlation report (short loop). |
| GET | `/api/meta/calibration/stratified`   | Per-stratum drift map (medium loop). |
| GET | `/api/meta/market-structure`         | Long-loop metric time series with year-ago comparisons. |
| GET | `/api/meta/counterfactual`           | REJECT-set shadow performance for the configured window. |
| GET | `/api/meta/overrides`                | Recent overrides + their realized outcomes. |
| GET | `/api/meta/threshold-proposals`      | Open proposals awaiting trader decision. |
| POST | `/api/meta/threshold-proposals/{id}` | Accept / reject / defer a proposal. Body carries the reason. |
| GET | `/api/meta/threshold-changes`        | Audit log of all past changes with their reasons. |
| POST | `/api/meta/overrides`               | Record a trader override (called from review UI). |

## 10. Over-fit guardrails

Every component above is built with the assumption that, left alone, it
would over-fit to the recent past. The guardrails:

1. **Minimum sample size per analysis**: 50 samples for stratified
   calibration, 30 for dimension attribution, 20 for override analysis.
   Below the floor, the analyzer reports "insufficient samples" with no
   numeric output the trader could anchor on.

2. **No auto-application**: see §5. Every threshold change requires explicit
   approval. There is no `auto-apply-when-confidence-above` switch and
   never will be.

3. **Reverted-change suppression**: when the trader accepts then reverts a
   change, the proposer back-offs from proposing similar changes for the
   same stratum for a cooldown period (default 90 days).

4. **Year-over-year baselines on the long loop**: market-structure metrics
   are compared not to last week's average but to the same period a year
   ago, so seasonal patterns don't trigger drift alerts.

5. **Stratum-level locking**: a proposal to tighten the
   `CONCENTRATED`-tier threshold doesn't move `STANDARD`-tier. Changes
   are scoped to the stratum that produced the evidence.

6. **Volatility-aware confidence intervals**: the CI on a proposed change
   widens when the underlying sample's variance widens. A noisy regime
   produces wider CIs and therefore weaker recommendations, which is the
   correct behavior.

## 11. What never gets auto-tuned

These are explicitly outside the feedback loop's scope:

- **The 8-layer MD framework itself.** The framework is the trader's
  mental model encoded into code. No analyzer rewrites it.
- **Hard gates.** Liquidity floor, blocklist, instrument type, staleness —
  these are non-negotiable rules, not parameters subject to calibration.
- **The verdict math.** Verdict = PASS / WATCH / REJECT computation logic
  is fixed. Only the *inputs* to that computation (scores, weights,
  thresholds) are subject to proposals.
- **Sensitive secrets or credentials.** Calibration data is for
  improving validation; it never reaches LLM providers, brokers, or any
  external system.

## 12. Privacy and what stays inside the system

All feedback data stays in red-dragon's persistence layer. Nothing about
realized outcomes, trader notes, override reasons, or threshold history
ever leaves the system. Even when the LLM scheduler is enabled, the
prompt explicitly excludes any historical performance data — the LLM
is a discovery source, not a feedback recipient.

The only exception is observability metrics (counts and aggregates of
proposals issued, accepted, rejected) which surface in `/actuator/metrics`
for operational health, never including symbol or trade details.

## 13. Implementation roadmap

Order chosen so each step produces standalone value even if the next is
deferred:

1. **Outcome enrichment** (Flyway V5) — capture stratification columns
   now so a year of history is in place when analyzers come online. Zero
   risk; pure data collection.

2. **Stratified calibration** (medium loop) — highest leverage with the
   least code. Reuses existing analyzer infrastructure; just groups before
   computing drift.

3. **Dimension attribution** (short loop) — independent of stratification.
   Pure correlation math. Adds the "which dimensions still work" answer.

4. **Trader override capture + analyzer** — Flyway V6 + listener +
   analyzer. Requires a small UI change in the static review page to
   record overrides.

5. **Threshold-change proposer + log** — Flyway V7 + the proposer service
   + the trader-facing accept/reject endpoints. Closes the loop on
   "system noticed drift → trader can act on it without leaving the app."

6. **Counterfactual replay** — Flyway V8 + nightly scheduler + REJECT-set
   analyzer. Depends on a deferred-execution flag on the candidate so
   we know which REJECT to replay (versus REJECTs that were also rejected
   by the trader for off-system reasons).

7. **Market-structure drift monitor** — nightly observation job. The most
   speculative; gives the framework-level questions a numeric voice but
   the metrics themselves will need a quarter or two of tuning before
   they're trustworthy.

Each step is a few hundred lines of code and one Flyway migration. None
of them block on another being deployed. They can land in whatever order
fits the calendar.

## 14. Out of scope (explicit)

- **Auto-tuning of any kind.** Every change is proposed; never applied.
- **Per-trade learned weights from realized outcomes.** Would over-fit to
  recent regime by construction. Use stratified manual proposals instead.
- **Trade-execution-layer feedback (slippage, fills, etc.).** Belongs to
  lib-execution when it exists, not to L8.
- **Trader behavior modeling beyond override capture.** Tempting and
  invasive; the override loop already captures the highest-signal portion
  of trader behavior without modeling personality or psychology.
- **External benchmark comparisons (S&P, sector ETFs).** Tells you whether
  the market was hot, not whether your framework is working. Defer until
  the inside-the-system feedback is mature.

## 15. Open decisions

1. **Stratum granularity.** Going too granular (regime × catalyst × tier
   = 75+ strata, most with n < 10) produces noise. Going too coarse
   (regime only) misses interactions. Current plan: track all
   combinations, but only surface drift for strata above the minimum
   sample size. Trader can toggle which strata appear on the dashboard.

2. **Override-text vectorization.** Requires running override reason
   notes through an LLM. Costs token quota; risks leaking trade-decision
   text to the LLM provider. Possible compromise: local embedding model
   only, kept inside the system. Defer until override volume justifies
   the investment.

3. **Counterfactual hold-period.** 14 days picked for first pass. Some
   trade theses need 30+ days to play out; some are intraday. Maybe
   compute multiple hold-period scenarios (3, 7, 14, 30 days) and report
   each.

4. **Year-over-year baselines vs rolling N-day**. Year-over-year is more
   robust to seasonality; rolling-N is more responsive to regime shifts.
   Show both, let the trader weigh them.

5. **Should the proposer ever propose *loosening* a threshold?** Asymmetric
   guardrails make sense: tightening is conservative, loosening adds
   capital risk. Maybe loosening proposals require a higher sample-size
   floor and a longer cooldown after acceptance. Currently planned:
   symmetric, but flagged in the proposal so the trader knows which
   direction the change pushes risk.

## 16. Notes for future-Claude

When implementing this, the high-risk pieces are:

- **Stratum capture at the right moment.** `regimeLabel` must be captured
  *at decision time*, not at outcome-ingest time. Outcomes can arrive
  weeks after the trade; the regime classification at that point is
  meaningless for calibration. Capture into `CalibrationOutcomeEntity`
  when the candidate goes through `CandidatePipelineOrchestrator`, not
  when the outcome arrives.

- **Confidence interval honesty.** The proposer must not paper over small
  samples with optimistic CIs. Use bootstrap resampling for asymmetric
  distributions; don't assume normality. A wide CI is *more* trustworthy
  than a narrow one with bad assumptions.

- **The override UI is the leverage point.** All the data design here
  presupposes that the trader actually records overrides with a reason
  text. The capture experience on the review page has to be one click +
  a short text box, or it won't happen.

- **Backwards-compatible migrations only.** Don't change existing columns
  on `calibration_outcome`. Add new nullable ones. The year of history
  the trader has already collected has no value if a migration drops it.

- **Never call this "ML" in code or comments.** It isn't. It's
  systematized human-in-the-loop feedback. Calling it ML invites the
  wrong abstractions and the wrong expectations.
