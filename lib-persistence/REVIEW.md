# lib-persistence — Implementation Review

**Date:** 2026-05-17
**Last fix pass:** 2026-05-26 (V14 validation verdict idempotency now uses an application-supplied deterministic fingerprint; V12 recursive CTE is PostgreSQL-correct with `WITH RECURSIVE`). Previous: 2026-05-25 (V14 idempotency unique keys); 2026-05-24 (V13 optimistic-lock @Version + audit columns); 2026-05-21 (V12 validation_verdict_reason child table); 2026-05-20 local fixes.
**Reviewer:** Automated audit pass
**Scope:** `lib-persistence/` — 14 JPA entities, 13 repositories, 1 mapper, 1 utility class, 9 Flyway migrations (now 10 after the fix pass).

---

## Fix Status

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | Critical | README documents only 3 of 9 migrations and 10 of 14 tables | **Fixed** — README now lists all 14 tables + V1–V10 migrations with a one-liner per file |
| 2 | High | `db/schema/` stale drafts with mismatched version numbers | **Fixed** — drafts moved to `db/drafts/` with a purpose-explaining README; legacy `db/schema/` retained as an empty tombstone-README pointing to the new location |
| 3 | High | Missing FKs on `backtest_result` and historical V1 tables | **Partially fixed** — new `V10__backtest_result_candidate_fk.sql` adds the missing FK + index for `backtest_result`; V9 already covered `market_snapshot` and `validation_verdict`. SAFETY note included in V10 about the orphan-row precheck operators must run |
| 4 | Medium | `validation_verdict.reason_codes` stored as varchar(4000) blob | **Fixed** — V12 creates `validation_verdict_reason` child table with FK (cascade delete) + indexes on `verdict_id` and `reason_code`; existing rows backfilled from the comma/pipe-separated blob columns via recursive-CTE split. New entity `ValidationVerdictReasonEntity`, new repo `ValidationVerdictReasonRepository` (supports `findByReasonCode` for L8 calibration drift queries). `ValidationVerdictEntity` now exposes `getReasons()` via `@OneToMany`; legacy `reasonCodes`/`explanations` columns marked `@Deprecated(forRemoval = true)` and no longer populated on writes. `PersistenceMapper.toValidationVerdictEntity` constructs child entities with back-references so cascade-save persists both. Three controllers (`CandidateHistoryController`, `CandidateReviewController`, `VerdictSummaryController`) migrated to read from `getReasons()` instead of splitting the legacy strings. Persistence test updated to assert against the new shape. Future V13 may drop the legacy columns once historical data is no longer needed in the original format. |
| 5 | Medium | No `@Version` optimistic locking | **Fixed** — V13 adds `version bigint default 0 not null` to all five mutable tables (`candidate`, `validation_verdict`, `verdict_override`, `trader_note`, `schwab_token`). Matching `@Version private long version` fields on the entities; Hibernate now increments on every UPDATE and throws `OptimisticLockException` on stale writes instead of silent last-write-wins. Read-only entities (market_bar, intraday_bar, market_snapshot, market_quote_observation, analytics_snapshot, backtest_result, calibration_outcome, trade_history_*) intentionally do not get a version column. |
| 6 | Medium | No `created_at`/`updated_at` audit columns | **Fixed** — V13 adds `created_at timestamp default current_timestamp not null` to the ten tables that lacked one and `updated_at timestamp default current_timestamp not null` to the five mutable tables. Entities expose `createdAt` as `insertable=false, updatable=false` (DB default fills it; never set from Java) and `updatedAt` via `@PreUpdate touchUpdatedAt()` + `@PrePersist onInsert()` hooks (refreshed on every mutation). Each entity also got a backwards-compatible secondary constructor matching the pre-V13 shape so the `PersistenceMapper` and controllers don't need positional-arg updates. Five new indexes on `created_at` for L8 calibration's "rows since X" queries. |
| 7 | Medium | README understates module size | **Fixed** — addressed alongside #1 |
| 8 | Medium | `@AllArgsConstructor` positional construction | **Fixed (high-traffic entities)** — `@Builder` added to `CandidateEntity`, `ValidationVerdictEntity`, `VerdictOverrideEntity`, `TraderNoteEntity`, `BacktestResultEntity`. Builders coexist with the existing `@AllArgsConstructor` (Lombok generates both), so JPA reflection-based reconstitution is unaffected. `PersistenceMapper.toCandidateEntity` and `toValidationVerdictEntity` migrated to the builder API; `VerdictOverrideController`, `TraderNoteController`, and `BacktestController` switched too. Remaining 9 entities are construction-light (mostly internal); adding the annotation later is a one-line per-entity change. |
| 9 | — | TRADE_DECISION.md confirmation | — (no action needed; doc and code aligned on "design only") |
| 10 | Medium | Audit-pattern inconsistency between `market_bar` and `intraday_bar` | **Fixed** — new `V11__market_bar_audit_column.sql` adds `created_at timestamp default current_timestamp not null`; `MarketBarEntity` mapped with `insertable = false, updatable = false` so the DB default populates; `PersistenceMapper.toMarketBarEntity` passes `null` for the new column |
| 11 | Medium | No idempotency keys on `validation_verdict` or `backtest_result` | **Fixed** — V14 adds `validation_verdict.idempotency_key` with unique index `uk_validation_verdict_idempotency_key`, plus `uk_backtest_result_run_candidate` on `(run_id, candidate_id)`. `PersistenceMapper` now writes a deterministic SHA-256 fingerprint for each validation verdict, so retried POSTs collide even though `created_at` changes at write time. Migration uses `if not exists` clauses so a partial-failure recovery just continues. SAFETY note in the migration header walks operators through duplicate-detection pre-checks. |
| 12 | Medium | Test coverage gap (5 of 6 areas uncovered) | Open — additive |
| 13 | Low | `@AllArgsConstructor` exposes generated id | Open |
| 14 | Medium | Schwab token plaintext | Open — security work, tracked separately |
| 15 | Low | `notes`/`explanations` columns silently truncatable | Open |
| 16 | — | Coverage note for `PersistenceStringUtils` | — |

**Open: 7** — `@AllArgsConstructor` id leakage on the remaining 9 entities, security work on Schwab token plaintext, additive test work, and the `notes`/`explanations` silent-truncation risk. The 5 most-constructed entities now have `@Builder`.

---

---

## Expected Behavior (per docs)

`lib-persistence/README.md` is the in-module spec, plus `TRADE_DECISION.md` for the planned (not yet implemented) trade-decision schema.

Key claims:

- Cross-cutting persistence: candidates, market bars, snapshots, verdicts, overrides, notes, backtest results, calibration outcomes, Schwab OAuth tokens (lines 14–17).
- Preserve source provenance and reasoning chains (line 17).
- Versioned and reviewable schema (line 18).
- **Current Tables (README lines 24–33)**: `candidate`, `market_bar`, `market_snapshot`, `analytics_snapshot`, `validation_verdict`, `verdict_override`, `trader_note`, `backtest_result`, `calibration_outcome`, `schwab_token`.
- **Migration files (README lines 53–55)** — explicitly lists three files:
  ```
  V1__candidate_pipeline_schema.sql
  V2__calibration_outcome.sql
  V3__schwab_token.sql
  ```
- "Treat the Flyway files under `db/migration/` as the runtime source of truth" (line 36).
- `TRADE_DECISION.md` is "design only. No Flyway migrations yet" (line 9–10).

ARCHITECTURE.md adds: lib-persistence is "JPA domain entities, repositories, mappers, and Flyway migrations" (line 63), cross-cutting.

---

## Files Reviewed

| Category | Files |
| --- | --- |
| Entities (`domains/`) | `CandidateEntity`, `MarketBarEntity`, `IntradayBarEntity`, `MarketSnapshotEntity`, `MarketQuoteObservationEntity`, `AnalyticsSnapshotEntity`, `ValidationVerdictEntity`, `VerdictOverrideEntity`, `TraderNoteEntity`, `BacktestResultEntity`, `CalibrationOutcomeEntity`, `SchwabTokenEntity`, `TradeHistoryImportBatchEntity`, `TradeHistoryRecordEntity` (14 total) |
| Mapper | `services/PersistenceMapper.java` |
| Repositories (`services/repositories/`) | 13 Spring Data repositories — one per entity except `TradeHistoryRecord` shares an importer. |
| Utility | `utilities/PersistenceStringUtils.java` |
| Flyway migrations (runtime) | V1–V9 in `db/migration/` |
| Flyway schema (reference drafts) | V1–V5 in `db/schema/` (numbering does not match runtime) |
| Tests | `services/PersistenceMapperTest.java` (only 1 file) |

---

## Findings

### 1. README "Current Tables" and "Current Package Layout" are severely outdated — three migrations listed, nine on disk; four tables exist that are not in the README

**Severity:** Critical
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-persistence\README.md:53-55` (lists V1, V2, V3)
- `C:\repos\red-dragon\red-dragon\lib-persistence\src\main\resources\db\migration\` (V1, V2, V3, V4, V5, V6, V7, V8, V9 — nine files)

**Expected vs Actual:** README:

```
lib-persistence/src/main/resources/db/migration
    V1__candidate_pipeline_schema.sql
    V2__calibration_outcome.sql
    V3__schwab_token.sql
```

Actual on-disk:

```
V1__candidate_pipeline_schema.sql
V2__calibration_outcome.sql
V3__schwab_token.sql
V4__analytics_snapshot.sql                              <-- not in README
V5__trader_notes.sql                                    <-- not in README
V6__verdict_overrides.sql                               <-- not in README
V7__market_snapshot_extended_fields.sql                 <-- not in README
V8__market_data_observations_and_trade_history.sql      <-- not in README
V9__core_pipeline_relationships.sql                     <-- not in README
```

README "Current Tables" lists 10 tables. V8 adds **4 more** tables (`intraday_bar`, `market_quote_observation`, `trade_history_import_batch`, `trade_history_record`) that the README does not acknowledge. Corresponding entities exist (`IntradayBarEntity`, `MarketQuoteObservationEntity`, `TradeHistoryImportBatchEntity`, `TradeHistoryRecordEntity`).

**Why it matters:** Three concrete problems:
1. **Onboarding misdirection.** A new developer reading the README will not know `intraday_bar` exists. Memory note `project_red_dragon_known_gaps.md` lists "frontend" as implemented in Tier 3 — the frontend depends on intraday data. The README hides it.
2. **The "runtime source of truth" line (line 36) is correct, but it's working around the README being out of date.** It should be the README, not a workaround comment.
3. **Trade-history schema is a significant addition** (`trade_history_import_batch` + `trade_history_record` with `realized_pnl`, `account`, `strategy_type`, `market_state` columns). This is referenced in `app` API endpoints (`/api/history/import`, `/api/history/imports`, `/api/history/trades`) but is undocumented in lib-persistence's README.

**Proposed fix:** Update README "Current Tables" to include all 14 tables, list all 9 migration files, and add a brief one-liner per recent migration explaining what it added. Treat the README as authoritative for what exists; the migration filenames alone don't tell the story.

---

### 2. `db/schema/` directory contains out-of-date schema drafts with non-matching version numbers

**Severity:** High
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-persistence\src\main\resources\db\schema\V1__candidate_pipeline_schema.sql`
- `db\schema\V2__analytics_snapshot.sql` (note: matches `db\migration\V4`)
- `db\schema\V3__trader_notes.sql` (matches `db\migration\V5`)
- `db\schema\V4__verdict_overrides.sql` (matches `db\migration\V6`)
- `db\schema\V5__market_snapshot_extended_fields.sql` (matches `db\migration\V7`)

**Expected vs Actual:** README line 35–36: *"There are also schema reference files under `db/schema/`. Treat the Flyway files under `db/migration/` as the runtime source of truth."*

The two folders have **mismatched version numbers** for the same DDL:
- `schema/V2__analytics_snapshot.sql` is `migration/V4__analytics_snapshot.sql`
- `schema/V3__trader_notes.sql` is `migration/V5__trader_notes.sql`
- `schema/V4__verdict_overrides.sql` is `migration/V6__verdict_overrides.sql`
- `schema/V5__market_snapshot_extended_fields.sql` is `migration/V7__market_snapshot_extended_fields.sql`

And `schema/` does not contain V8 (intraday bars / market observations / trade history) or V9 (FK relationships). The `schema/` folder is stale.

**Why it matters:**
1. **Confusion for a developer searching for DDL.** A grep for the table name finds two files; only one is run.
2. **Flyway-managed migrations are immutable** — once V4 ran in production, it cannot be changed. But the `schema/` V2 is the same DDL with a different version number, which would imply two attempts to land the same change.
3. **Tooling risk.** If any code path accidentally loads `schema/` instead of `migration/`, Flyway will see "duplicate version" or "checksum mismatch" errors.

**Proposed fix:** Either (a) delete `db/schema/` entirely — `db/migration/` is canonical per the README; (b) rename `schema/` to `db/design/` or `db/drafts/` to make its purpose explicit; or (c) keep `schema/` but enforce that its file names match migration version numbers exactly.

---

### 3. Foreign-key constraints to `candidate` are missing on several child tables; partial fix added late in V9

**Severity:** High
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-persistence\src\main\resources\db\migration\V1__candidate_pipeline_schema.sql:32-77`
- `C:\repos\red-dragon\red-dragon\lib-persistence\src\main\resources\db\migration\V9__core_pipeline_relationships.sql:1-16`

**Expected vs Actual:** Per README "Preserve source provenance and reasoning chains" (line 17), a snapshot/verdict row must always reference a real candidate. Actual:

- `market_snapshot` (V1): had `candidate_id` column but no FK. Added in V9 (line 1–3).
- `validation_verdict` (V1): had `candidate_id` column but no FK. Added in V9 (line 5–7).
- `backtest_result` (V1): has `candidate_id` column. **Still no FK constraint** in V9.
- `analytics_snapshot` (V4): defined with FK from creation (V4 line 13–14). ✓
- `trader_note` (V5): defined with FK from creation (per V5 file). ✓ (assumed; not re-read here)
- `verdict_override` (V6): defined with FK from creation. ✓ (assumed)
- `market_quote_observation` (V8): no `candidate_id` column — fine, it's symbol-keyed.
- `intraday_bar` (V8): no `candidate_id` — fine.

So `backtest_result.candidate_id` is referentially un-enforced — a backtest result row can reference a deleted/never-existed candidate. The original V1 tables `market_snapshot` and `validation_verdict` were also un-enforced for ~6 months until V9 landed.

**Why it matters:**
1. **Orphaned `backtest_result` rows are silently insertable.** A typo or stale candidate id produces a row with no `candidate` to join against; the UI then renders "Unknown" or NPEs.
2. **Pre-V9 `market_snapshot` and `validation_verdict` rows may contain orphans.** V9 added the FK but does not validate existing data; if any orphan rows exist, the migration would have failed *or* the database may have a deferred constraint silently disabled. Worth checking.

**Proposed fix:** Add the missing FK in a new migration:

```sql
-- V10__backtest_result_candidate_fk.sql
alter table backtest_result
    add constraint fk_backtest_result_candidate
    foreign key (candidate_id) references candidate(candidate_id);
```

Run a `SELECT count(*) FROM backtest_result WHERE candidate_id NOT IN (SELECT candidate_id FROM candidate)` before deploy to confirm no orphans. Do the same for `market_snapshot` and `validation_verdict` historical data (the V9 migration should have included that check).

---

### 4. `validation_verdict.reason_codes` and `explanations` are stored as `varchar(4000)` and `varchar(8000)` blobs

**Severity:** Medium
**Files:**
- `C:\repos\red-dragon\red-dragon\lib-persistence\src\main\resources\db\migration\V1__candidate_pipeline_schema.sql:49-59`
- `C:\repos\red-dragon\red-dragon\lib-persistence\src\main\java\dev\reddragon\persistence\domains\ValidationVerdictEntity.java:42-46`

**Expected vs Actual:** README line 17 "Preserve source provenance and reasoning chains" implies queryable structure. Actual: reason codes and explanations are serialized as concatenated strings:

```java
@Column(name = "reason_codes", length = 4000)
private String reasonCodes;

@Column(name = "explanations", length = 8000)
private String explanations;
```

Query "find every candidate rejected because of CATALYST_NOT_CREDIBLE" requires `WHERE reason_codes LIKE '%CATALYST_NOT_CREDIBLE%'` — a full-table scan with no index help, and false-positive risk if a future reason code is a prefix of another.

Also: 4000 / 8000 character limits are arbitrary. A particularly verbose explanation chain could be truncated silently.

**Why it matters:** The L8 calibration loop (per `lib-analytics/META_ADAPTATION_FEEDBACK.md`) proposes per-reason-code drift analysis. Doing that against `LIKE '%X%'` is operationally infeasible at scale and fragile against truncation.

**Proposed fix:** Normalize into a child table:

```sql
-- V10__validation_verdict_reason.sql
create table validation_verdict_reason (
    verdict_id bigint not null references validation_verdict(id) on delete cascade,
    reason_code varchar(64) not null,
    explanation varchar(2000),
    sort_order smallint not null
);
create index idx_validation_verdict_reason_code on validation_verdict_reason(reason_code);
```

This is a normal one-to-many. Migration can backfill from the existing concatenated strings.

---

### 5. No `@Version` (optimistic locking) on any entity

**Severity:** Medium
**Files:** all entity classes under `domains/`.

**Expected vs Actual:** README lines 11–17 list 10 tables; none of the entities have a `@Version` field. Specifically:
- `VerdictOverrideEntity` — a trader overriding the same candidate's verdict twice in quick succession could race.
- `TraderNoteEntity` — concurrent edits could clobber.
- `SchwabTokenEntity` — token refresh from two threads could write inconsistent state (the application is single-instance today, so latent — but still a concern when scheduled refresh and ad-hoc refresh coincide).
- `CandidateEntity` — read-only mostly, but could be updated for headline/summary corrections.

**Why it matters:** Concurrent updates produce last-write-wins silently. JPA's optimistic locking would throw `OptimisticLockException` to the caller for explicit handling.

**Proposed fix:** Add `@Version private long version;` to mutable entities (`VerdictOverride`, `TraderNote`, `SchwabToken`). Audit which entities are mutable — read-only entities don't need it. Migration:

```sql
alter table verdict_override add column version bigint default 0 not null;
alter table trader_note     add column version bigint default 0 not null;
alter table schwab_token    add column version bigint default 0 not null;
```

---

### 6. Most tables lack `created_at` / `updated_at` audit columns

**Severity:** Medium
**Files:** Migrations V1, V4, V8.

**Expected vs Actual:** README line 17–18 emphasizes provenance. The pattern in V1:

- `candidate.observed_at` — when the catalyst was observed in the outside world. Not when the row was written.
- `market_bar` — no audit column; rows are date-keyed (`bar_date`), but no `inserted_at`.
- `validation_verdict.created_at` — present. ✓
- `backtest_result.tested_at` — present (semantic, not audit). Acceptable.
- `market_snapshot` — `observed_at` (semantic). No `created_at`.

V8's `intraday_bar.created_at` (line 11) is present — pattern was learned, not retrofitted to V1 tables.

**Why it matters:** "When did this row arrive in our database vs when did the world produce it" is the difference between provenance and audit. The L8 loop wants both. Without `created_at` on market_bar, "we received this bar 3 hours late" is unknowable.

**Proposed fix:** Add a one-shot migration adding `created_at timestamp default current_timestamp not null` to every table that doesn't have one. Lombok JPA-Auditing (`@CreatedDate`, `@LastModifiedDate`) can populate.

---

### 7. Schema layouts diverge from README's documented "Current Package Layout"

**Severity:** Medium
**File:** `lib-persistence/README.md:43-56`

**Expected vs Actual:** README:

```
lib-persistence/src/main/java/dev/reddragon/persistence
    domains/      JPA entities
    services/     PersistenceMapper
    services/repositories/ Spring Data repositories
    utilities/    persistence helpers
```

Actual: matches the structure. ✓ But: 14 entities, not "JPA entities" as a generic; the `utilities/` directory contains exactly one file (`PersistenceStringUtils.java`); `services/PersistenceMapper.java` is the only file at that level (no other "services" exist). README is correct in structure but undersells the size of `domains/`.

**Why it matters:** Minor. Could note table count and rough purpose.

**Proposed fix:** Add to README: "14 entities; see `domains/` for the full list."

---

### 8. `PersistenceMapper` is the only mapper but `@AllArgsConstructor` on entities makes mapping positional

**Severity:** Medium
**File:** `C:\repos\red-dragon\red-dragon\lib-persistence\src\main\java\dev\reddragon\persistence\domains\CandidateEntity.java:18` (and siblings)

**Expected vs Actual:** Every entity uses `@AllArgsConstructor` plus `@Getter` plus `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. The mapper constructs entities positionally:

```java
// PersistenceMapper (hypothetical based on common pattern, not verbatim)
new CandidateEntity(
    candidate.candidateId(),
    candidate.symbol(),
    candidate.companyName(),
    catalystTypeAsString,
    sourceTypeAsString,
    candidate.sourceId(),
    candidate.sourceUrl(),
    candidate.observedAt(),
    candidate.headline(),
    candidate.summary()
);
```

A reorder of two String fields in `CandidateEntity` (e.g., `companyName` and `sourceId`) would compile silently and produce wrong data. Same risk applies to every entity.

**Why it matters:** Identical risk pattern to lib-backtest Finding #5 and lib-domain Finding #12.

**Proposed fix:** Use `@Builder` on entities, or replace `@AllArgsConstructor` with named static factory methods.

---

### 9. `TradeDecisionMd` schema is design-only — code state matches doc state ✓

**Severity:** N/A — confirmation
**File:** `C:\repos\red-dragon\red-dragon\lib-persistence\TRADE_DECISION.md:9-10`

The README explicitly says (lines 58–62) that the trade-decision schema is design only. `TRADE_DECISION.md` line 9–10 confirms: "design only. No Flyway migrations yet." No corresponding tables exist in `db/migration/`; no `TradeDecisionEntity` exists under `domains/`. Code and doc are aligned.

---

### 10. `intraday_bar` and `market_quote_observation` tables have `created_at` but `market_bar` does not — inconsistent audit pattern across the same conceptual domain

**Severity:** Medium
**Files:**
- `db/migration/V1__candidate_pipeline_schema.sql:17-27` (market_bar, no created_at)
- `db/migration/V8__market_data_observations_and_trade_history.sql:1-13` (intraday_bar, created_at present)

**Expected vs Actual:** Both tables store time-series market data and should have parallel schemas. `intraday_bar` has `created_at`, `market_bar` does not. Either both should have it or neither; the inconsistency reads like the V8 author learned a lesson V1 didn't have.

**Why it matters:** Same provenance argument as Finding #6.

**Proposed fix:** Backport `created_at` to `market_bar` in V10.

---

### 11. `validation_verdict` has no semantic uniqueness key; duplicate writes are possible

**Severity:** Medium
**File:** `db/migration/V1__candidate_pipeline_schema.sql:49-62`

**Expected vs Actual:**

```sql
create table validation_verdict (
    id bigint generated by default as identity primary key,
    candidate_id varchar(64) not null,
    ...
    created_at timestamp not null
);
```

Only the synthetic `id` is unique. A retry of `POST /api/pipeline/manual` for the same `candidate_id` would insert a second verdict row with a different `id` and the same `candidate_id`. The L8 calibration loop would then count both as separate samples.

Memory note `project_red_dragon_known_gaps.md`: "Tier 1-3 items implemented (idempotency, ...)" — but `validation_verdict` has no uniqueness check enforcing idempotency at the DB layer. Idempotency must be done by the application code; the DB does not protect.

**Why it matters:** A network retry, a duplicate event, or a developer test loop can pollute the verdict history. The `L8` loop's "rolling win rate" is sample-count-sensitive; duplicate samples skew it.

**Implemented fix:** V14 adds nullable `validation_verdict.idempotency_key` plus a unique index over that key. New writes populate it with a deterministic SHA-256 fingerprint of the semantic validation output, so a retry collides even if `created_at` differs. `backtest_result` uses the natural `(run_id, candidate_id)` key.

---

### 12. Test coverage is one file — `PersistenceMapperTest` — none of the README's six expected coverage areas have a dedicated test

**Severity:** Medium
**File:** `lib-persistence/src/test/java/dev/reddragon/persistence/services/PersistenceMapperTest.java`

**Expected vs Actual:** README lines 64–68:

> Tests should cover entity mapping, repository behavior, migration validity, required fields and constraints, relationship integrity, and storage of reason codes and source metadata.

Actual: one test file. `PersistenceMapperTest` covers the mapper round-trip. No tests for:
- **Migration validity** — no test runs Flyway against a fresh schema and asserts the final state matches entity expectations.
- **Required fields and constraints** — no test asserts that `candidate_id NOT NULL` is enforced.
- **Relationship integrity** — no test asserts that V9 FK constraints actually fire on orphan inserts.
- **Repository behavior** — no test for any of the 13 repositories.
- **Storage of reason codes** — the varchar(4000) concatenation pattern (Finding #4) has no test.

**Why it matters:** Migrations land in production with no automated validation. An entity-DDL drift is only caught when the application starts and Hibernate complains — late, in some non-deterministic order.

**Proposed fix:** Add a `@DataJpaTest`-based integration test class per major entity. Run Flyway via Testcontainers (or H2 for fast feedback) and assert (a) Flyway version is current, (b) `EntityManager.find` round-trips work, (c) constraints fire.

---

### 13. `@AllArgsConstructor` exposes all fields including `id` (generated identity)

**Severity:** Low
**Files:** all entities with `@GeneratedValue(strategy = GenerationType.IDENTITY)`.

**Expected vs Actual:** `CandidateEntity` is keyed by a domain `candidateId` (no generated id), but `MarketSnapshotEntity`, `ValidationVerdictEntity`, `BacktestResultEntity`, `CalibrationOutcomeEntity`, etc., have a generated `Long id` field. `@AllArgsConstructor` requires the caller to pass `id` — which they cannot meaningfully do for a yet-to-be-persisted row, so they pass `null`.

**Why it matters:** Mild boilerplate. Mappers must remember to pass `null` for the id field; if they accidentally pass a value, JPA may treat it as a `MERGE` instead of `INSERT`.

**Proposed fix:** Use `@Builder` and let callers omit `id` for new rows. Or use `@RequiredArgsConstructor` plus setters for the optional id (less safe).

---

### 14. `SchwabTokenEntity` exists, persists tokens — but per memory `project_red_dragon_known_gaps.md`, security is deferred

**Severity:** Medium (informational)
**File:** `lib-persistence/src/main/java/dev/reddragon/persistence/domains/SchwabTokenEntity.java`

**Expected vs Actual:** README lines 14–17 list `schwab_token` as one of the supported tables. The entity exists. The migration `V3__schwab_token.sql` creates the table. But:

- Schwab access tokens are sensitive credentials.
- The audit memory note: "security + deployment + lib-execution deferred" (`project_red_dragon_known_gaps.md`).
- There is no documented encryption-at-rest column type, no `@Convert` to apply field-level encryption, no comment about how secrets are protected.

**Why it matters:** Persisting bearer tokens in a plain `varchar` column means anyone with DB read access has trading-account access. Even in a single-user dev environment, an inadvertent backup or query log entry leaks the token.

**Proposed fix:** Apply field-level encryption via a Hibernate `AttributeConverter` (e.g., `@Convert(converter = EncryptedStringConverter.class)`). Or document that the H2 file is encrypted at the filesystem layer. Track this against the documented security gap.

---

### 15. `notes` and `explanations` columns of 4000 / 8000 chars are silently truncatable

**Severity:** Low
**Files:**
- `db/migration/V1__candidate_pipeline_schema.sql:46` (`market_snapshot.notes varchar(4000)`)
- `db/migration/V1__candidate_pipeline_schema.sql:56-57` (`validation_verdict.reason_codes/explanations`)
- `db/migration/V8__market_data_observations_and_trade_history.sql:27` (`market_quote_observation.notes`)
- `db/migration/V8__market_data_observations_and_trade_history.sql:38` (`trade_history_import_batch.warnings varchar(8000)`)

**Expected vs Actual:** JPA does not error on insertion of a `String` longer than the column. With H2/Postgres the DB engine will reject; with some configurations (and older H2) it silently truncates. Either way, a 4001-char note is data loss.

**Why it matters:** Long adversarial-finding chains, especially with the AdversarialValidationAnalyzer outputting 10 separate notes per candidate, plus per-scorer commentary, can grow. 4000 is not generous.

**Proposed fix:** Either (a) use `CLOB` / `TEXT` for these blobs, or (b) normalize as discussed in Finding #4. (a) is cheaper.

---

### 16. `PersistenceStringUtils` is a one-file utility — its contents not reviewed in depth here

**Severity:** N/A — coverage note
**File:** `lib-persistence/src/main/java/dev/reddragon/persistence/utilities/PersistenceStringUtils.java`

Not read line-by-line. Likely contains the helpers for the varchar-blob concatenation/split (Finding #4). Worth a follow-up review if Finding #4 is acted upon.

---

## Entity / Schema Drift Table

| Entity | DDL location | Drift |
| --- | --- | --- |
| `CandidateEntity` | V1 | ✓ matches |
| `MarketBarEntity` | V1 | ✓ matches (no created_at — Finding #10) |
| `IntradayBarEntity` | V8 | ✓ matches |
| `MarketSnapshotEntity` | V1 + V7 extension | ✓ matches; `relative_volume`/`vwap_deviation`/`directional_persistence` correctly nullable for pre-V7 rows |
| `MarketQuoteObservationEntity` | V8 | ✓ matches |
| `AnalyticsSnapshotEntity` | V4 | ✓ matches; FK from V4 |
| `ValidationVerdictEntity` | V1 + V9 FK | ✓ matches; FK added late (Finding #3) |
| `VerdictOverrideEntity` | V6 | Likely ✓ (not deep-read) |
| `TraderNoteEntity` | V5 | Likely ✓ (not deep-read) |
| `BacktestResultEntity` | V1 | **No FK to candidate** (Finding #3) |
| `CalibrationOutcomeEntity` | V2 | ✓ matches (CalibrationOutcomeEntity contains 8 score fields per V2) |
| `SchwabTokenEntity` | V3 | ✓ matches but plain-text storage concern (Finding #14) |
| `TradeHistoryImportBatchEntity` | V8 | ✓ matches |
| `TradeHistoryRecordEntity` | V8 | ✓ matches; FK to import batch present |

---

## Doc/Code Drift Summary

| Claim | Reality |
| --- | --- |
| 3 migration files | 9 (Finding #1) |
| 10 tables in README | 14 actual tables (Finding #1) |
| `db/schema/` is "reference files" | Out-of-date drafts with mismatched version numbers (Finding #2) |
| FK to candidate enforced | Missing on `backtest_result` (Finding #3) |
| Versioned and reviewable schema | True for V1–V9 ordering; no test asserts migration validity (Finding #12) |
| `TRADE_DECISION.md` is design-only | True ✓ |
| 6 areas of testing expected | Only mapping covered (Finding #12) |

---

## Strengths

1. **All entities follow a consistent shape:** `@Entity @Table @Getter @NoArgsConstructor(PROTECTED) @AllArgsConstructor`. Easy to read, easy to template. The PROTECTED no-args constructor matches JPA's requirements without exposing it to callers.
2. **`@GeneratedValue(strategy = GenerationType.IDENTITY)`** is consistent across surrogate keys. Postgres and H2 both handle this efficiently.
3. **Migrations are forward-only and version-numbered.** No retroactive edits to V1 evident; new fields are added via new migrations (V7 adds market_snapshot fields), which is correct Flyway practice.
4. **`MarketSnapshotEntity` correctly types extension fields as `Double` (boxed) rather than `double`** because V7 added them as nullable for pre-existing rows. Boxing is the right call.
5. **Indexes are present on every `(symbol, timestamp_desc)` access pattern** — `idx_candidate_symbol_observed_at`, `idx_market_bar_symbol_date`, `idx_intraday_bar_symbol_start_time`, `idx_analytics_snapshot_symbol_observed`, `idx_validation_symbol_created_at`, `idx_calibration_outcome_symbol_observed_at`. The read pattern was thought through.
6. **`unique(symbol, bar_date)` on `market_bar`** — natural-key idempotency for daily bars. Same on `intraday_bar(symbol, start_time)`. Correct.
7. **V9 retroactively adds FK constraints** rather than leaving the gaps — improving referential integrity in a backward-compatible way. The migration also adds the index on the new FK column (line 9–13 of V9), which is needed because Postgres doesn't auto-index FKs.
8. **`CalibrationOutcomeEntity` correctly captures the 8 score dimensions at decision time** (V2 lines 13–20) plus the realized outcome (lines 23–26). This is exactly what the L8 calibration analyzer needs — the snapshot is preserved alongside the outcome, not derived from a moving target.
9. **`TRADE_DECISION.md` is honest about its status** (design only). No vaporware tables sit in `db/migration/`; no half-implemented entities clutter `domains/`.
10. **Schwab access-token entity exists** and has a clear single-row purpose. The interface for token refresh (per lib-marketdata) doesn't leak Schwab-specific DTOs through here; the persistence layer just stores the strings.

---

## Summary table — issue count by severity

| Severity | Count | Findings |
| --- | --- | --- |
| Critical | 1 | #1 (README massively outdated on migrations and tables) |
| High | 2 | #2 (stale `db/schema/` with mismatched versions), #3 (missing FKs on `backtest_result` and historical V1 tables) |
| Medium | 8 | #4 (reason-code varchar blob), #5 (no `@Version`), #6 (no audit columns), #7 (README understates), #8 (positional `@AllArgsConstructor`), #10 (audit-pattern inconsistency), #11 (no idempotency keys), #12 (test coverage gaps), #14 (Schwab token plaintext) |
| Low | 3 | #13, #15, #16 |
| **Total** | **14** | |

The headline finding is **#1**: the README documents 30% of the actual schema — 3 migrations of 9, 10 tables of 14. A reader following the README will not learn about the intraday-bar feed, the market-quote observation feed, or the trade-history import feature, all of which are real and live in `app` endpoints today. Bringing the README current is the single highest-leverage doc fix for the whole module.

**#3** (missing FKs) is the most consequential correctness issue: `backtest_result.candidate_id` is referentially un-enforced, so orphan rows are insertable. A V10 migration adding the FK + a one-time `DELETE FROM backtest_result WHERE candidate_id NOT IN (SELECT candidate_id FROM candidate)` (after auditing) closes the gap.

**#14** (Schwab token plain-text) sits in the deferred-security bucket per project memory — known and intentional, but worth keeping visible until lib-execution lands.
