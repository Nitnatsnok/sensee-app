# shared/database

SQLDelight 2.x layer for the Sensee SQLite database. Schemas live in their owning feature
modules and are merged into a single `SenseeDatabase` here:
- `shared/feature/library/database-schema` — catalog tables (lemma/card/deck/deck_card),
  `catalogEntityQueries`.
- `shared/feature/practice/database-schema` — SRS tables (srs_card/srs_review_log),
  `practiceSrsEntityQueries`.
- `shared/settings/database-schema`, `shared/tts/database-schema`.

Schema modules are owned by the feature that owns the data: catalog is Library-owned,
SRS review state is Practice-owned. They are FK-independent, so they live in separate
schema modules merged into the one aggregate `SenseeDatabase` (generated row types stay
in the neutral `app.sensee.core.database` package).

## Runtime config

`DatabaseConfig.resetOnSchemaMigration`, supplied by each host (Android/Desktop/iOS/Web),
controls the dev-only auto-reset behavior. When `true`, `reconcileDevSchema` fingerprints
the schema's `create` script and stores it in a `sensee_dev_schema` marker table; on the
next launch a changed fingerprint means the `.sq` files moved, so it drops every user
object and recreates from the current schema. **Never enable in production builds — it
destroys user data.**

The trigger is *actual schema drift*, not the SQLDelight schema version. The version is
`(highest .sqm number) + 1` and never changes when you edit a `.sq` file in place — the
exact dev workflow this exists for — so a version-gated reset would silently never fire.
Fingerprinting the create script sidesteps that.

Current wiring:
- Android: derived from `ApplicationInfo.FLAG_DEBUGGABLE` in `MainActivity`.
- iOS: `kotlin.native.Platform.isDebugBinary` in `IosRootHolder`.
- Desktop: hardcoded `true` in `apps/desktopApp/src/main/kotlin/app/sensee/main.kt` (no
  release desktop build exists yet — flip to `false` when one ships).
- Web: defaults to `false` (and the web driver bypasses SQLDelight's migrate path anyway).

## Adding a schema change

While the schema is churning (current state), edit the `.sq` file directly and let
`reconcileDevSchema` recreate the dev DB on the next launch. No `.sqm` and no version
bump. Do not create placeholder migrations just for churn.

Once real users ship and we have data worth keeping, switch to the production path
below and drop `resetOnSchemaMigration` from all callers.

Production path (not active yet):

1. **Edit the `.sq` file** in the relevant `database-schema` module. Add/modify
   `CREATE TABLE`, columns, indexes, etc.
2. **Write a migration** in the same module under
   `src/commonMain/sqldelight/migrations/<oldVersion>.sqm`. The file's number is the
   version it migrates *from*. Example: `1.sqm` migrates schema v1 to v2.
   - Schema version is inferred from `(highest .sqm number) + 1`.
   - Use plain SQL: `ALTER TABLE ...`, `CREATE INDEX ...`, etc. No SQLDelight DSL.
   - SQLite limits: cannot drop or rename a column, cannot change a column type, cannot
     add a column with a non-constant default. Rebuild the table via temp table + copy
     when those limits hit.
3. **Regenerate the schema snapshot:**
   ```
   ./gradlew :shared:database:generateCommonMainSenseeDatabaseSchema
   ```
   This drops a new `<version>.db` file into `src/commonMain/sqldelight/databases/`.
   Commit it.
4. **Verify migrations compile:**
   ```
   ./gradlew :shared:database:verifyCommonMainSenseeDatabaseMigration
   ```
   This walks every `.sqm` against the snapshots and asserts the result matches the
   current `.sq` files. Runs automatically as part of `build`.
5. **Test the migration** if it touches data (backfill, type widening, splitting tables).
   Add a JVM unit test next to `DevResetSchemaTest` using `JdbcSqliteDriver.IN_MEMORY`:
   open an older snapshot via `<n>.db`, run `SenseeDatabase.Schema.migrate(driver, n, n+1)`,
   assert the expected rows/columns.

## Why the snapshot in VCS?

The `<version>.db` snapshots are the source of truth for what each schema version looked
like. `verifyMigrations` replays migrations against them and would otherwise have nothing
to compare to. Treat them like generated lockfiles — never edit by hand, always
regenerate.

## Why `reconcileDevSchema` exists

While the schema is churning early in the project, hand-writing `.sqm` files for every
column tweak is wasteful. `reconcileDevSchema` lets dev builds wipe and recreate the DB
whenever the create script changes, with no `.sqm` or version bump required. Once the
schema stabilizes and we have real users with data worth keeping, drop
`resetOnSchemaMigration` from all callers (or guard it behind a build flag stricter than
"debug") so that missing migrations become loud build failures instead of silent data
wipes.
