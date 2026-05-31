# Agent Instructions for `shared/lexicon`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for the neutral lexical model.

## Scope

Applies to:
- `shared/lexicon/...`

## Local context

`shared/lexicon` contains:
- `domain` - `Sense`, lexical read models, and repository contracts.
- `enrichment` - anti-corruption mapping from provider-agnostic AI enrichment into `Sense`.
- `serialization` - shared DTO mapping for persisted `Sense` data.

## Local rules

- `Sense` is the target shape for confirmed lexical material. AI enrichment produces candidates that map into `Sense`, not the other way around.
- Keep `domain` pure: no DI, persistence implementation, AI wire DTOs, or feature dependencies.
- Features that need rich lexical material depend on `lexicon/domain` or `lexicon/enrichment`, not another feature's domain model.
- Keep Vocabulary Editor and Library converging on the same central `Sense` model before persistence/projection.

## Local verification

- For enrichment mapper changes, run mapper tests and include unknown-taxonomy behavior when relevant.
- For serialization changes, also follow `shared/lexicon/serialization/AGENTS.md`.

## Do not

- Do not introduce feature-private candidate state into `lexicon/domain`.
- Do not make Library or Practice own the canonical lexical sense shape.
- Do not bypass the shared enrichment mapper when converting AI suggestions to `Sense`.

## Related skills

- `.agents/skills/dictionary-enrichment-schema-review`
- `.agents/skills/kmp-module-boundary-review`
