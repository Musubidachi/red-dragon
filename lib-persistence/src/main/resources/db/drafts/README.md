# db/drafts/

These are **pre-migration design drafts**. They are NOT applied at runtime —
Flyway only scans `lib-persistence/src/main/resources/db/migration/`.

## Why these exist

Each file here represents an early sketch of a schema change before it was
promoted to a versioned migration. Their `Vn__` prefixes intentionally do
**not** match the runtime version numbers in `db/migration/`. For example,
the analytics_snapshot draft is `drafts/V2__analytics_snapshot.sql` but
landed at runtime as `migration/V4__analytics_snapshot.sql` after earlier
migrations were inserted before it.

## When to delete a draft

Once a draft has been promoted to a real migration and the migration has
shipped, the corresponding draft can be deleted from this folder. They are
kept here primarily as a historical artifact of how the schema evolved.

## Do not edit migrations after they ship

Flyway-managed migrations are immutable. New changes go in a new `Vn__` file
under `db/migration/`. Use this drafts folder for the exploratory phase only.
