# lib-persistence — Trade Decision Schema

Architecture doc for the entity family that captures a human's deliberate
decision to enter a trade, the broker orders that decision spawns, and the
fills that result. This is the bridge between machine-produced
`validation_verdict` (from `lib-validation`) and broker-bound order placement
(from `lib-execution`).

> **Status**: design only. No Flyway migrations yet. DDL below is illustrative —
> when we promote this to code, the canonical schema lives in
> `lib-persistence/src/main/resources/db/migration/`.

---

## 1. Where this fits

```
[ ingestion ]     produces candidate
[ analytics ]     produces regime_snapshot, asymmetry_score
[ validation ]    produces validation_verdict
[ HUMAN ]         creates trade_decision  ← this doc
[ execution ]     submits order → receives fill(s)
```

A `trade_decision` is the durable record of intent. Everything before it is
machine-produced (and revisable as algorithms improve). The `trade_decision`
is the user's call to act and is treated as immutable once confirmed — intent
changes are captured as append-only revisions, not in-place updates.

---

## 2. Entity overview

```
account ─┐
         │
         ├─< trade_decision ─< trade_decision_leg (deferred: multi-leg)
         │           │
         │           ├─< trade_decision_event   (lifecycle timeline, append-only)
         │           ├─< trade_decision_revision (intent changes, append-only)
         │           └─< order ─< fill
         │
         └─< (future: position, statement, ...)
```

| Table                      | Purpose                                                                 | Mutability                       |
| -------------------------- | ----------------------------------------------------------------------- | -------------------------------- |
| `account`                  | The brokerage account(s) we operate against.                            | Rarely changes.                  |
| `trade_decision`           | The user's confirmed intent to enter a trade.                           | Immutable once `CONFIRMED`.      |
| `trade_decision_leg`       | One leg of a multi-leg order. Deferred; designed for future use.        | Immutable.                       |
| `trade_decision_event`     | Lifecycle events (CONFIRMED, SUBMITTED, PARTIAL, FILLED, …). Append-only. | Append-only.                     |
| `trade_decision_revision`  | Amendments to original intent (raised stop, etc.). Append-only.         | Append-only.                     |
| `order`                    | One broker-bound order created from a decision. May be 1:N per decision. | Status updates allowed.          |
| `fill`                     | One executed fill against an order. Many fills can settle one order.    | Immutable once written.          |

---

## 3. `account` table

Forward-compat. Even though there's one Roth IRA today, modeling it as a
table avoids a painful migration later when a second account appears.

```sql
CREATE TABLE account (
    account_id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    broker              VARCHAR(32)     NOT NULL,        -- 'schwab' for now
    broker_account_hash VARCHAR(128)    NOT NULL,        -- Schwab's opaque account hash
    nickname            VARCHAR(64)     NOT NULL,        -- 'roth-ira', 'taxable', etc.
    account_type        VARCHAR(32)     NOT NULL,        -- ROTH_IRA / TRADITIONAL_IRA / TAXABLE / etc.
    is_active           BOOLEAN         NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    UNIQUE (broker, broker_account_hash)
);
```

Notes:
- `broker_account_hash` is opaque from the broker (Schwab uses an opaque hash,
  not the raw account number). Map maintenance lives in `lib-execution`.
- `account_type` matters for hard-rule validation (e.g., no short selling in
  an IRA).

---

## 4. `trade_decision` — the main table

```sql
CREATE TABLE trade_decision (
    trade_decision_id       UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              UUID            NOT NULL REFERENCES account(account_id),

    -- instrument
    ticker                  VARCHAR(16)     NOT NULL,
    instrument_type         VARCHAR(16)     NOT NULL,    -- 'EQUITY' | 'OPTION' | 'MULTI_LEG_OPTION'
    option_right            VARCHAR(4),                  -- 'CALL' | 'PUT'  (nullable; option only)
    option_strike           NUMERIC(20, 4),              -- nullable; option only
    option_expiry           DATE,                        -- nullable; option only
    option_occ_symbol       VARCHAR(32),                 -- nullable; the full OCC symbol if known

    -- direction
    side                    VARCHAR(24)     NOT NULL,    -- 'BUY' | 'SELL' | 'SELL_TO_OPEN' | 'BUY_TO_CLOSE' | ...

    -- thesis (the structured + free-text combo)
    thesis_type             VARCHAR(48)     NOT NULL,    -- from the taxonomy in §10
    rationale               TEXT            NOT NULL,    -- free-text "why I'm doing this"
    conviction_score        SMALLINT        NOT NULL CHECK (conviction_score BETWEEN 1 AND 5),

    -- intended plan
    time_horizon            VARCHAR(16)     NOT NULL,    -- 'INTRADAY' | 'SWING' | 'POSITION' | 'LONG_TERM'
    order_type              VARCHAR(16)     NOT NULL,    -- 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT' | 'TRAILING_STOP'
    order_duration          VARCHAR(8)      NOT NULL,    -- 'DAY' | 'GTC' | 'IOC' | 'FOK'

    -- price plan (entry, stop, targets as JSONB array)
    entry_price             NUMERIC(20, 4),              -- nullable for MARKET orders
    stop_loss_price         NUMERIC(20, 4),              -- the stop. Separate from price_targets by design.
    price_targets           JSONB           NOT NULL DEFAULT '[]'::jsonb,  -- shape: [{ "price": 52.0, "exit_pct": 50, "deadline": "2026-06-30" }, …]

    -- sizing
    planned_quantity        NUMERIC(20, 4)  NOT NULL,     -- shares or contracts depending on instrument
    quantity_unit           VARCHAR(16)     NOT NULL,     -- 'SHARES' | 'CONTRACTS' | 'DOLLARS'
    max_acceptable_loss_usd NUMERIC(20, 2),               -- explicit dollar risk
    sizing_rationale        VARCHAR(32)     NOT NULL,     -- 'FIXED_PCT' | 'RISK_PARITY' | 'CONVICTION_TIER' | 'KELLY_LITE' | 'GUT'

    -- lineage (links to upstream artifacts that surfaced or approved this trade; nullable for off-pipeline trades)
    candidate_id            UUID,                         -- → candidate(candidate_id) when that table lands
    validation_verdict_id   UUID,                         -- → validation_verdict(validation_verdict_id) when it lands
    regime_snapshot_id      UUID,                         -- → regime_snapshot(regime_snapshot_id) when it lands

    -- frozen-at-decision snapshot of upstream state (see §11)
    pre_trade_snapshot      JSONB           NOT NULL DEFAULT '{}'::jsonb,

    -- lifecycle status — also maintained via trade_decision_event for the timeline
    status                  VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',  -- DRAFT|CONFIRMED|SUBMITTED|WORKING|PARTIAL|FILLED|CLOSED|CANCELLED|REJECTED

    -- broker tie-ins (populated as the trade progresses)
    submitted_at            TIMESTAMPTZ,
    first_filled_at         TIMESTAMPTZ,
    closed_at               TIMESTAMPTZ,

    -- retrospective (populated when status = CLOSED)
    realized_pnl_usd        NUMERIC(20, 2),
    actual_holding_period_seconds BIGINT,
    outcome_notes           TEXT,

    -- categorization
    tags                    TEXT[]          NOT NULL DEFAULT ARRAY[]::TEXT[],

    -- audit
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    -- constraints
    CHECK (
        (instrument_type = 'EQUITY')
        OR (instrument_type IN ('OPTION', 'MULTI_LEG_OPTION') AND option_right IS NOT NULL AND option_strike IS NOT NULL AND option_expiry IS NOT NULL)
    ),
    CHECK (
        order_type <> 'MARKET' OR entry_price IS NULL
    )
);

CREATE INDEX trade_decision_account_created_idx ON trade_decision (account_id, created_at DESC);
CREATE INDEX trade_decision_ticker_created_idx  ON trade_decision (ticker, created_at DESC);
CREATE INDEX trade_decision_status_idx          ON trade_decision (status);
CREATE INDEX trade_decision_thesis_idx          ON trade_decision (thesis_type);
CREATE INDEX trade_decision_open_idx            ON trade_decision (status)
    WHERE status IN ('CONFIRMED','SUBMITTED','WORKING','PARTIAL','FILLED');
```

### Design notes

- **No `decision_time` column** — `created_at` covers it. Per Fab on 2026-05-10:
  decision time is not particularly interesting on its own; the lifecycle
  timestamps (`submitted_at`, `first_filled_at`, `closed_at`) carry the
  information that matters.
- **`status` is denormalized** from `trade_decision_event` for fast queries.
  Both are written atomically when a transition happens. If they diverge, the
  event log wins.
- **Option fields live on the row** for the common case (single-leg). The
  `trade_decision_leg` table (§5) carries them for multi-leg.
- **`tags` is `text[]`** rather than a many-to-many tag table. Fewer joins, fine
  for personal-tool scale. The risk is spelling drift — mitigate with a
  validator at write time that normalizes case and warns on unknown tags.

---

## 5. `trade_decision_leg` — multi-leg hook

Multi-leg option orders (verticals, spreads, condors, etc.) are deferred from
v1 per the lib-execution doc. The table exists in the schema so the model is
forward-compatible without a destructive migration.

```sql
CREATE TABLE trade_decision_leg (
    leg_id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_decision_id   UUID            NOT NULL REFERENCES trade_decision(trade_decision_id) ON DELETE CASCADE,
    leg_index           SMALLINT        NOT NULL,
    side                VARCHAR(24)     NOT NULL,     -- BUY_TO_OPEN / SELL_TO_OPEN / etc.
    instrument_type     VARCHAR(16)     NOT NULL,     -- 'EQUITY' | 'OPTION'
    ticker              VARCHAR(16)     NOT NULL,
    option_right        VARCHAR(4),
    option_strike       NUMERIC(20, 4),
    option_expiry       DATE,
    option_occ_symbol   VARCHAR(32),
    quantity            NUMERIC(20, 4)  NOT NULL,
    UNIQUE (trade_decision_id, leg_index)
);
```

For single-leg trades (the v1 case), the `trade_decision` row's instrument
fields are authoritative and no `trade_decision_leg` rows are inserted.
For `MULTI_LEG_OPTION` instruments, the row-level instrument fields can be
null (or hold the "primary" leg) and the legs table is the source of truth.

---

## 6. `trade_decision_event` — lifecycle timeline

Every status transition is a row. The main `trade_decision.status` column is
derived from the latest event but kept in sync for query speed.

```sql
CREATE TABLE trade_decision_event (
    event_id            UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_decision_id   UUID            NOT NULL REFERENCES trade_decision(trade_decision_id) ON DELETE CASCADE,
    event_type          VARCHAR(32)     NOT NULL,     -- 'CONFIRMED' | 'SUBMITTED' | 'BROKER_ACCEPTED' | 'PARTIAL_FILL' | 'FILLED' | 'CANCELLED' | 'REJECTED' | 'CLOSED'
    occurred_at         TIMESTAMPTZ     NOT NULL DEFAULT now(),
    detail              JSONB           NOT NULL DEFAULT '{}'::jsonb,    -- e.g. {"filled_qty":100, "avg_price":47.18, "reason":"manual cancel"}
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX trade_decision_event_decision_idx ON trade_decision_event (trade_decision_id, occurred_at);
```

Reasoning: the timeline is the truth. A row that says `status = FILLED` doesn't
tell you *when* it filled, in how many pieces, or whether there was a partial
in between. The event table does.

---

## 7. `trade_decision_revision` — intent amendments

When intent changes after `CONFIRMED` — most commonly raising a stop, scaling
the target, or widening the time horizon — the change is appended here rather
than overwriting the original row.

```sql
CREATE TABLE trade_decision_revision (
    revision_id         UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_decision_id   UUID            NOT NULL REFERENCES trade_decision(trade_decision_id) ON DELETE CASCADE,
    revised_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),

    -- which fields changed: a JSONB diff
    -- shape: { "stop_loss_price": { "from": 45.00, "to": 47.50 }, "rationale": { "from": "...", "to": "..." } }
    change_set          JSONB           NOT NULL,

    -- why the intent changed
    revision_reason     TEXT            NOT NULL,
    revised_by          VARCHAR(32)     NOT NULL DEFAULT 'user'   -- 'user' for now; future: 'auto-trail-stop' etc.
);

CREATE INDEX trade_decision_revision_decision_idx ON trade_decision_revision (trade_decision_id, revised_at);
```

At read time, the "current intent" is the original `trade_decision` row with
each revision's `change_set` applied in `revised_at` order. Convenience view
(implemented later when needed):

```sql
-- pseudo-DDL; actual implementation can use a function or a materialized view
CREATE VIEW trade_decision_current AS
    SELECT td.*, /* fields overridden by latest revision */
      FROM trade_decision td
      LEFT JOIN LATERAL (…apply revisions…) cur ON true;
```

For v1, applying revisions in application code is fine — don't over-engineer
the view layer until there's pressure to.

---

## 8. `order` — broker-bound orders

A `trade_decision` *may* spawn multiple orders (scaling in, OCO pairs, etc.),
so `order` is its own table.

```sql
CREATE TABLE "order" (
    order_id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_decision_id         UUID            NOT NULL REFERENCES trade_decision(trade_decision_id) ON DELETE RESTRICT,

    -- idempotency: generated client-side BEFORE the broker call
    broker_client_order_id    VARCHAR(64)     NOT NULL UNIQUE,
    -- broker's own id, populated after acceptance
    broker_order_id           VARCHAR(64)     UNIQUE,

    -- order shape — repeats some fields from trade_decision because broker reality may differ
    side                      VARCHAR(24)     NOT NULL,
    order_type                VARCHAR(16)     NOT NULL,
    order_duration            VARCHAR(8)      NOT NULL,
    limit_price               NUMERIC(20, 4),
    stop_price                NUMERIC(20, 4),
    quantity                  NUMERIC(20, 4)  NOT NULL,
    quantity_unit             VARCHAR(16)     NOT NULL,

    -- lifecycle (echoed in fills, but materialized here for fast queries)
    status                    VARCHAR(16)     NOT NULL,    -- PENDING|SUBMITTED|WORKING|PARTIAL|FILLED|CANCELLED|REJECTED
    submitted_at              TIMESTAMPTZ,
    accepted_at               TIMESTAMPTZ,
    terminal_at               TIMESTAMPTZ,                  -- when it left the working state

    -- broker response payload (raw, for audit)
    broker_response_payload   JSONB,

    created_at                TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX order_decision_idx          ON "order" (trade_decision_id);
CREATE INDEX order_broker_order_id_idx   ON "order" (broker_order_id);
CREATE INDEX order_status_idx            ON "order" (status);
```

### Idempotency

`broker_client_order_id` is generated *before* the POST to Schwab and is the
unique key on this table. If a POST fails ambiguously (network drop), the
recovery procedure (per the lib-execution doc) is **GET orders, look for this
client_order_id, reconcile** — never re-POST. The UNIQUE constraint enforces
that we only ever have one local record per intent-to-submit.

Note: `order` is a SQL keyword. Quote it in DDL/queries or rename to
`broker_order` if you prefer. I'd quote it; the domain term is clearer.

---

## 9. `fill` — actual executions

```sql
CREATE TABLE fill (
    fill_id           UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID            NOT NULL REFERENCES "order"(order_id) ON DELETE RESTRICT,
    executed_at       TIMESTAMPTZ     NOT NULL,
    quantity          NUMERIC(20, 4)  NOT NULL CHECK (quantity > 0),
    price             NUMERIC(20, 4)  NOT NULL,
    commission_usd    NUMERIC(20, 4)  NOT NULL DEFAULT 0,
    fees_usd          NUMERIC(20, 4)  NOT NULL DEFAULT 0,
    broker_fill_id    VARCHAR(64),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX fill_order_idx         ON fill (order_id);
CREATE INDEX fill_executed_at_idx   ON fill (executed_at DESC);
```

Fills are immutable. Schwab's API gives us fill events; we record them and
never modify them. Realized P&L on the `trade_decision` is the aggregate of
its order's fills, computed at close time.

---

## 10. The `thesis_type` taxonomy

Initial vocabulary, derived from the catalysts that actually move tickers in
the user's style (premium-selling + concentrated thematic conviction +
catalyst-driven momentum). The taxonomy is intentionally wide to start; prune
unused values after the first quarter of data.

| Value                      | Description                                                          |
| -------------------------- | -------------------------------------------------------------------- |
| `earnings`                 | Earnings event-driven (beat, miss, guidance)                         |
| `contract_partnership`     | New material agreement (8-K Item 1.01); strategic partnership        |
| `mna`                      | Announced or expected M&A activity                                   |
| `governance`               | Officer/director change, listing event, structural shift             |
| `severe_negative`          | Bankruptcy, delisting, restatement, impairment, going-concern        |
| `dilution`                 | Equity issuance, S-1/S-3/424B, unregistered sales                    |
| `insider_buy`              | Form 4 P-coded purchase; insider conviction signal                   |
| `activist_13d`             | Schedule 13D filing with influence-of-control intent                 |
| `institutional_13g`        | Schedule 13G passive institutional accumulation                      |
| `technical_breakout`       | Pure price-action setup (breakout, range expansion)                  |
| `mean_reversion`           | VWAP / range mean-reversion trade                                    |
| `premium_collection`       | Covered call / cash-secured put — selling premium on existing thesis |
| `macro_thematic`           | Sector or macro driver (commodity, rate, policy)                     |
| `meme_momentum`            | High-attention / retail-flow / narrative momentum                    |
| `gut`                      | Discretionary call without a structured upstream signal              |

Use a Postgres ENUM type or a `VARCHAR(48)` + `CHECK (thesis_type IN (…))`.
ENUM is type-safer; adding values requires `ALTER TYPE`. CHECK is more flexible.
For a personal tool: VARCHAR + CHECK, with a constants class in
`lib-persistence` that the rest of the app references.

---

## 11. `pre_trade_snapshot` JSONB shape

Frozen at decision time so future-you can audit the decision without the
analytics layer's evolution clouding the picture. Suggested fields:

```json
{
  "snapshot_at": "2026-05-10T14:32:11Z",

  "instrument": {
    "price": 47.21,
    "bid": 47.20,
    "ask": 47.22,
    "spread_bps": 4.2,
    "session": "RTH"
  },

  "features": {
    "vwap_distance_pct": -1.4,
    "atr_14": 1.87,
    "realized_vol_30d": 0.42,
    "range_position_today": 0.32,
    "volume_vs_20d_avg_ratio": 1.6
  },

  "regime": {
    "label": "rotational_equilibrium",
    "confidence": 0.71,
    "regime_snapshot_id": "…"
  },

  "validation": {
    "verdict": "PASS",
    "asymmetry_score": 0.68,
    "factors": [
      {"name": "catalyst_materiality", "weight": 0.4, "value": 0.9},
      {"name": "narrative_freshness", "weight": 0.3, "value": 0.6}
    ],
    "validation_verdict_id": "…"
  },

  "account": {
    "equity_usd": 135820.93,
    "buying_power_usd": 28430.11,
    "existing_position_qty": 0
  }
}
```

Validate JSONB shape application-side. Don't fight Postgres for JSON-schema
enforcement; just write it once and stick to it. If the shape evolves later,
version the snapshot (`"schema_version": 2`) rather than rewriting historical
rows.

---

## 12. Indexes and constraints recap

- `trade_decision`: indexes on `(account_id, created_at DESC)`,
  `(ticker, created_at DESC)`, `status`, `thesis_type`, plus a partial index
  on open trades for the dashboard "open positions" query.
- `trade_decision_event`: `(trade_decision_id, occurred_at)` covers most
  reads.
- `trade_decision_revision`: `(trade_decision_id, revised_at)`.
- `"order"`: `trade_decision_id`, `broker_order_id`,
  `broker_client_order_id UNIQUE`, `status`.
- `fill`: `order_id`, `executed_at DESC`.

Constraints to enforce in DDL (don't push to app code):

- `trade_decision`: option fields required iff `instrument_type IN ('OPTION',
  'MULTI_LEG_OPTION')`. `entry_price NULL` iff `order_type = 'MARKET'`.
- `"order"`: `broker_client_order_id` is `UNIQUE`. This is the load-bearing
  idempotency invariant.
- `fill`: `quantity > 0`. `executed_at NOT NULL`.

---

## 13. Flyway migration sequencing

When this lands in code, sequence migrations in this order so each step is
independently reversible:

1. `V001__create_account.sql` — `account` table.
2. `V002__create_trade_decision.sql` — `trade_decision` + indexes.
3. `V003__create_trade_decision_event.sql` — event log.
4. `V004__create_trade_decision_revision.sql` — revisions.
5. `V005__create_order.sql` — `"order"` table + unique constraint.
6. `V006__create_fill.sql` — `fill` table.
7. `V007__create_trade_decision_leg.sql` — multi-leg hook (can land later;
   nothing else depends on it).

Each migration is self-contained DDL only. Data migrations come later, in
separately-numbered `V###__data_*` files.

---

## 14. Out of scope (explicit)

- **Position table.** A "current position" view is derivable from open
  `trade_decision` rows joined to `fill`s. Materialize later only if the
  query is slow.
- **Statement reconciliation table.** If we ever want to reconcile against
  Schwab statements, that's a separate ingestion concern that lives outside
  this schema.
- **Tax-lot tracking.** Schwab does this. Don't re-implement.
- **Cross-account aggregation.** One account today; aggregation logic lives
  in analytics, not in this schema.

---

## 15. Open decisions

1. **`order` vs `broker_order` table name.** SQL keyword collision. Quote
   the name, or rename. I'd quote; clearer at the domain level.
2. **ENUM vs `VARCHAR + CHECK`** for `thesis_type`, `side`, `status`, etc.
   Recommend `VARCHAR + CHECK` for personal-tool flexibility; revisit if
   the taxonomy stabilizes.
3. **`pre_trade_snapshot` versioning.** Add `"schema_version"` from day one
   so future evolution is non-destructive? Recommend yes.
4. **Numeric precision.** `NUMERIC(20, 4)` for prices, `NUMERIC(20, 8)` for
   crypto if we ever go there. Current choice covers equity + options with
   room to spare.
5. **Reconciling `trade_decision.status` with `trade_decision_event`.** Write
   them together in one transaction (single DB call from application
   code)? Or have a trigger maintain `status` from the event log? Recommend
   transaction-level for explicitness; triggers hide control flow.
6. **Should `trade_decision_revision` be applied virtually (read-time merge)
   or materialized into a `trade_decision_current` table?** Virtual for v1;
   materialize when the read pattern justifies it.

---

## 16. Notes for future-Claude

- This was scoped as "design doc only" by Fab on 2026-05-10. No Flyway
  migration files exist yet. Lineage FKs (`candidate_id`,
  `validation_verdict_id`, `regime_snapshot_id`) point to tables that aren't
  designed yet; leave them as plain `UUID` columns without `REFERENCES` until
  those tables exist, then add the FK constraints in a later migration.
- Fab's chosen reasoning shape is **structured taxonomy + free-text**
  (§4 + §10), confirmed on 2026-05-10. Resist the urge to switch to free-text
  + tags without re-asking.
- Intent amendments are **append-only revisions** (`trade_decision_revision`),
  confirmed on 2026-05-10. The `trade_decision` row itself is treated as
  immutable after `CONFIRMED`. The exception is the lifecycle status fields
  (`status`, `submitted_at`, `first_filled_at`, `closed_at`) — those mutate
  in lockstep with `trade_decision_event` writes, in the same transaction.
- Decision time is intentionally *not* a separate column; `created_at`
  carries it.
