# lib-persistence

`lib-persistence` owns Red Dragon's database model and persistence contracts.

This module should answer one question:

> What system state must be stored so candidates, enrichment, analytics, validation, and review decisions can be audited later?

Persistence should support explanation and repeatability. The project is not tracking a portfolio or placing orders, but it still needs durable records of what was evaluated and why.

## Responsibilities

- Define JPA entities and repositories.
- Own Flyway database migrations.
- Store candidate, enrichment, analytics, and validation snapshots.
- Preserve source provenance and reasoning chains.
- Keep schema changes versioned and reviewable.

Planned tables from the root roadmap:

- `candidate`
- `enrichment_snapshot`
- `regime_snapshot`
- `validation_verdict`
- `market_bar`

## Non-responsibilities

This library should not:

- Fetch external data.
- Score candidates.
- Decide validation verdicts.
- Contain web controllers.
- Contain the Spring Boot application main class.
- Track portfolio state, orders, fills, or account balances.

## Expected flow

```text
pipeline event or snapshot
    -> persistence-facing model / entity mapper
    -> repository
    -> database table
    -> review / audit / later analysis
```

## Schema design guidance

### Store snapshots, not just current state

The trader should be able to see why a candidate received a verdict at the time it was reviewed. Avoid overwriting important context that belongs to a past decision.

### Preserve provenance

Candidate and enrichment records should retain source identifiers, timestamps, and source descriptions. This is critical for debugging and trust.

### Keep verdicts explainable

Validation records should store:

- final verdict
- aggregate score, if used
- reason codes
- display-ready explanation text
- timestamp
- references to candidate and enrichment snapshots

### Treat migrations as the source of truth

Schema changes should happen through Flyway migrations. Entity changes and migration changes should be committed together.

## Suggested package layout

```text
lib-persistence
└── src/main/java/dev/reddragon/persistence
    ├── entity          # JPA entities
    ├── repository      # Spring Data repositories
    ├── mapper          # domain/snapshot to entity mapping
    └── support         # persistence utilities

src/main/resources/db/migration
    └── V1__initial_schema.sql
```

## First implementation target

1. Add initial Flyway migration for core tables.
2. Create entities for candidate and validation verdict.
3. Add repositories for candidate lookup and verdict history.
4. Store source provenance and reason codes in a simple format first.
5. Expand to enrichment and market-bar history after the first ingestion source works.

## Testing expectations

Tests should cover:

- entity mapping
- repository save/find behavior
- migration validity
- required fields and constraints
- relationship integrity between candidates and snapshots
- storage of reason codes and source metadata

Prefer focused repository tests once the database runtime is wired in the application module.
