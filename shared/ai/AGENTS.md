# Agent Instructions for `shared/ai`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for the AI enrichment seam.

## Scope

Applies to:
- `shared/ai/...`

## Local context

`shared/ai` owns provider-agnostic enrichment contracts, curated enrichment fixtures, LLM integration, and routing composition. ADR-005 is the canonical architecture decision for this seam.

## Local rules

- Keep `core` provider-agnostic and feature-agnostic: no AI vendor SDKs and no feature domain types.
- Keep AI output as candidates. The seam never produces canonical content: a suggestion maps to an unsaved `Sense` in the editor (a candidate is just a `Sense` the user has not confirmed yet), and confirmation is the user saving the selected sense. The seam neither persists nor auto-promotes.
- Treat availability as first-class. `Unavailable` and `Degraded` are normal results; do not throw malformed provider responses across the seam.
- Keep the wire DTO versioned. Schema changes need deliberate mapping updates and `commonTest` coverage.
- Keep routing in `integration`: curated enrichment wins for covered default-language requests; LLM fills the tail when a key/settings path enables it.
- Edit curated coverage as JSON fixtures under `shared/ai/curated-enrichment/src/commonMain/mockFixtures/enrichment/`; do not edit generated fixture code.

## Local verification

- For core schema/mapper changes, run the affected `commonTest` task or module check.
- For curated fixture shape changes, run the curated-enrichment fixture tests in the affected module.
- For routing changes, run the `integration` tests before widening to broader checks.

## Do not

- Do not leak provider SDKs, feature domain types, or concrete settings UI concerns into `shared/ai/core`.
- Do not silently accept unknown schema changes without mapper/test updates.
- Do not route topic-steered requests to curated examples unless the routing decision is changed deliberately and documented.

## Related skills

- `.agents/skills/dictionary-enrichment-schema-review`
- `.agents/skills/kmp-module-boundary-review`
- `.agents/skills/architecture-docs-sync`
