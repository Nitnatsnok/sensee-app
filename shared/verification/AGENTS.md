# Agent Instructions for `shared/verification`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for lexical verification.

## Scope

Applies to:
- `shared/verification/...`

## Local context

`shared/verification` owns the lexical-verification seam. Rationale and authoring detail live in `README.md`, `AUTHORING.md`, ADR-007, and ADR-009; keep this file as the operational agent overlay.

Modules follow the shared seam convention:
- `core` - provider-agnostic contracts and evidence models.
- `sensee-curated` - Sensee-owned reference facets served through the mock backend.
- `free-dictionary`, `datamuse`, `languagetool` - third-party network adapters.
- `database-schema` - SQLDelight cache schema.
- `integration` - routing, catalog, cache composition, and Metro providers.

## Local rules

- Keep `core` free of dictionary SDKs and feature domain types.
- Verification emits evidence, findings, and optional suggested actions; it never silently rewrites user material.
- Treat `Unavailable` and `Degraded` as normal results.
- Respect `LicensePolicy` on every source. `storeContentAllowed` gates L2 persistence; `usableAsLlmContext` gates evidence reuse for LLM context.
- Multi-word units are first-class. Do not collapse phrasal verbs, idioms, fixed phrases, and single words into one generic lookup result.
- Keep one adapter per external source. New third-party sources need their own adapter module, descriptor, and source-catalog/test update.
- Sensee-owned reference data belongs in `sensee-curated` mock fixtures and keeps the backend-served shape documented in `AUTHORING.md`.

## Local verification

- For provider/catalog changes, run the affected module tests and source-catalog completeness test.
- For cache/schema changes, also follow `shared/database/AGENTS.md` and `.agents/skills/sqldelight-schema-aggregation-review`.
- For fixture authoring changes, run the fixture shape tests in `sensee-curated`.

## Do not

- Do not add bundled third-party dictionary corpora or on-device vendor datasets.
- Do not persist responses from sources unless every contributing source permits `storeContentAllowed`.
- Do not send source content to LLM context unless the source permits `usableAsLlmContext`.
- Do not let an uncatalogued source become persistable; fail closed.

## Related skills

- `.agents/skills/dictionary-enrichment-schema-review`
- `.agents/skills/kmp-module-boundary-review`
- `.agents/skills/sqldelight-schema-aggregation-review`
- `.agents/skills/architecture-docs-sync`
