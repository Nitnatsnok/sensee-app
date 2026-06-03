# Agent Instructions for `shared/grammar`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for the neutral grammar taxonomy.

## Scope

Applies to:
- `shared/grammar/...`

## Local context

`shared/grammar` contains:
- `domain` - pure value types and provider contracts.
- `data` - taxonomy wire DTOs, mock fixtures, source adapter, and cached projections.

The broader taxonomy rationale is in ADR-006 and `docs/domain/pos-and-forms.adoc`.

## Local rules

- Keep taxonomy domain types forward-compatible: sealed types need an `Unknown(id)` branch and total `fromId(id)` parsing.
- Treat id strings as wire/storage contracts. Renaming an id is a data migration, not a refactor.
- Keep pair invariants runtime-resolved. `null` invariants mean pass-through; non-null invariants mean strict filtering at the AI boundary.
- Preserve asymmetric degraded values: `TaxonomyInvariantsProvider.invariants()` may return `null`; `GrammarLabelsProvider.labels()` may return `GrammarLabels.EMPTY`.
- Keep preload ownership in the startup feature. Grammar data providers own loading/caching, not splash orchestration.

## Local verification

- For domain changes, run the affected grammar module tests.
- For data/fixture changes, also follow `shared/grammar/data/AGENTS.md`.
- For AI-boundary taxonomy changes, include tests for unknown ids and strict/pass-through behavior.

## Do not

- Do not crash or drop unknown taxonomy ids by default.
- Do not put Compose, persistence, DI side effects, or feature dependencies into `domain`.
- Do not make `vocabulary-editor` depend on downstream catalog/library models for grammar taxonomy.

## Related skills

- `.agents/skills/dictionary-enrichment-schema-review`
- `.agents/skills/kmp-module-boundary-review`
- `.agents/skills/architecture-docs-sync`
