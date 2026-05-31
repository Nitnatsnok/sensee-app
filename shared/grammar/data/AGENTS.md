# Agent Instructions for `shared/grammar/data`

This file extends the root `AGENTS.md` and `shared/grammar/AGENTS.md`.
Follow those rules first; this file only adds local rules for taxonomy data.

## Scope

Applies to:
- `shared/grammar/data/...`

## Local context

This module owns the taxonomy wire DTO, the `practice/grammar/taxonomy` mock fixture, the source adapter, and cached projections into `GrammarLabels` and `TaxonomyInvariants`.

## Local rules

- This module is the only `practice/grammar/taxonomy` `MockFixtureSet` contributor. A duplicate key collides in `MergedFixtureReader`.
- Keep projection builders pure: `GrammarTaxonomyDto.toGrammarLabels()` and `toTaxonomyInvariants()` must not perform Compose, persistence, or DI work.
- Keep labels multi-language by BCP-47 tag. Adding a UI language is a fixture extension in `src/commonMain/mockFixtures/practice/grammar/taxonomy.json`.
- Edit the JSON fixture, not generated Kotlin under `build/generated/`.

## Local verification

- Run the affected module tests after fixture or projection changes.
- For mock fixture generation failures, inspect the generated output only for diagnosis; do not edit it.

## Do not

- Do not add a second fixture provider for `practice/grammar/taxonomy`.
- Do not hardcode UI language assumptions into `domain`.
- Do not move caching or preload orchestration into projection builders.

## Related skills

- `.agents/skills/dictionary-enrichment-schema-review`
- `.agents/skills/kmp-source-set-review`
