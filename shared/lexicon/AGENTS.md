# Agent Instructions for `shared/lexicon`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for the neutral lexical model.

## Scope

Applies to:
- `shared/lexicon/...`

## Local context

`shared/lexicon` contains:
- `domain` - `Sense`, lexical read models, and repository contracts.
- `data` / `database-schema` - the canonical `sense` store (SQLDelight) and the repository implementations.
- `serialization` - shared DTO mapping (`SenseDto`) for persisted and service-wire `Sense` data.

The AI-enrichment anti-corruption mapper (`EnrichmentSuggestion -> Sense`) lives in its sole consumer, `shared/feature/vocabulary-editor/domain` (`mapping/`), not here — capture is the only producer that maps AI output into the canon.

## Local rules

- `Sense` is the target shape for confirmed lexical material. AI enrichment produces candidates that map into `Sense`, not the other way around.
- Keep `domain` pure: no DI, persistence implementation, AI wire DTOs, or feature dependencies.
- Features that need rich lexical material depend on `lexicon/domain` or `lexicon/serialization`, not another feature's domain model.
- Keep Vocabulary Editor and Library converging on the same central `Sense` model before persistence/projection.

## Local verification

- For serialization changes, also follow `shared/lexicon/serialization/AGENTS.md`.

## Do not

- Do not introduce feature-private candidate state into `lexicon/domain`.
- Do not make Library or Practice own the canonical lexical sense shape.

## Related skills

- `.agents/skills/dictionary-enrichment-schema-review`
- `.agents/skills/kmp-module-boundary-review`
