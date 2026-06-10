---
name: sqldelight-schema-aggregation-review
description: Use this skill when Sensee work touches SQLDelight .sq/.sqm files, feature-owned database-schema modules, shared/database aggregation, migrations, repositories, query ownership, verification/TTS/settings/schema tables, or database tests. Covers the physical SQLDelight layer and aggregation, not logical AI/verification schema contracts (dictionary-enrichment-schema-review).
---

# SQLDelight Schema Aggregation Review

Status: active.

## Operating mode

Review/support. Check ownership and migration implications before recommending SQL or repository changes.

## Non-goals

- Do not move feature-owned schema into `shared/database`.
- Do not hand-edit generated SQLDelight output or snapshots.

## Workflow

1. Read `shared/database/AGENTS.md` and affected schema module files.
2. Identify the owning feature or seam for every table/query.
3. Check whether the current dev-reset workflow or production migration path applies.
4. Verify aggregation through `shared/database` and repository boundaries.

## Check

- Table and schema ownership is explicit.
- `shared/database` aggregates runtime database access without becoming feature domain (ADR 0002).
- Features do not access another feature's tables directly without a contract.
- `.sq` edits match the current dev workflow; `.sqm` migrations appear only when production migration path is needed.
- Generated snapshots under `shared/database/src/commonMain/sqldelight/databases/` are regenerated, never hand-edited.
- Queries do not create hidden cross-feature coupling.
- Transaction boundaries are clear; multi-statement writes run inside one `transaction`/`transactionWithResult`.
- Reactive reads are exposed as Flows (`asFlow()` from `coroutines-extensions`) and generated row types are mapped to domain models, not returned across module boundaries.
- Migration/query tests are updated when behavior changes.
- LikeC4/docs are updated for significant persistence ownership changes.

## Verification

- Module-scoped check; the schema generate/verify task names are documented in `shared/database/AGENTS.md`:
  ```shell
  .\gradlew.bat :shared:database:check
  ```

## Output

- Schema ownership
- Cross-feature access risks
- Migration impact
- LikeC4/docs impact
- Test gaps
- Recommended minimal fix

## Related skills

For repository Flow and coroutine shape, use `kotlin-flow-state-event-modeling` and `kotlin-coroutines-structured-concurrency` when available in the running agent.
