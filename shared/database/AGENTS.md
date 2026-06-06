# Agent Instructions for `shared/database`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for the SQLDelight aggregate database.

## Scope

Applies to:
- `shared/database/...`

## Local context

`shared/database` aggregates feature-owned and seam-owned SQLDelight schemas into one `SenseeDatabase`. Schema ownership stays in the owning feature/seam module:
- library catalog: `shared/feature/library/database-schema`;
- practice SRS state: `shared/feature/practice/database-schema`;
- settings/TTS/verification schema modules: `shared/settings/database-schema`, `shared/tts/database-schema`, `shared/verification/database-schema`.

## Local rules

- Keep runtime database access and platform drivers here; keep feature table ownership in the owning schema module.
- Generated SQLDelight row types stay in the neutral `app.sensee.core.database` package.
- `DatabaseConfig.resetOnSchemaMigration` is dev-only. Never enable schema auto-reset for production builds because it destroys user data.
- During current schema churn (`early-stage`; tightening trigger in arc42 §11), edit `.sq` files directly and rely on dev reset. Do not create placeholder `.sqm` migrations for churn.
- When production migrations become active, regenerate snapshots with SQLDelight and include the generated `<version>.db` snapshot in the same change.

## Local verification

- Aggregate database checks:
  ```shell
  .\gradlew.bat :shared:database:check
  ```
- Production migration snapshot generation, when applicable:
  ```shell
  .\gradlew.bat :shared:database:generateCommonMainSenseeDatabaseSchema
  .\gradlew.bat :shared:database:verifyCommonMainSenseeDatabaseMigration
  ```
- Add a JVM migration test next to `DevResetSchemaTest` when a migration transforms existing data.

## Do not

- Do not hand-edit SQLDelight generated sources or snapshot `.db` files.
- Do not move feature-owned queries into `shared/database` just for convenience.
- Do not enable destructive dev reset outside development/debug wiring.

## Related skills

- `.agents/skills/sqldelight-schema-aggregation-review`
- `.agents/skills/kmp-module-boundary-review`
- `.agents/skills/architecture-docs-sync`
