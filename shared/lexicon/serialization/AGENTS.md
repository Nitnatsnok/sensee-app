# Agent Instructions for `shared/lexicon/serialization`

This file extends the root `AGENTS.md` and `shared/lexicon/AGENTS.md`.
Follow those rules first; this file only adds local rules for shared lexical DTOs.

## Scope

Applies to:
- `shared/lexicon/serialization/...`

## Local context

This module owns the shared serialized shape of `Sense` and bidirectional mapping between `Sense` and `SenseDto`. It is intentionally neutral and non-feature-owned.

## Local rules

- Keep only shared DTOs and mappers here: `SenseDto`, sub-DTOs, `Sense.toDto()`, and `SenseDto.toDomain()`.
- Keep the package outside `*.data.*`; feature data layers may depend on this shared boundary without violating feature-private data layering.
- Re-resolve ids through the same neutral parsing/fromId APIs used by the AI boundary.
- Reads stay forward-compatible: unknown taxonomy ids surface as `Unknown(id)`, not dropped values.

## Local verification

- Run serialization mapper tests after DTO or mapping changes.
- If persisted shape changes affect SQL schema or migrations, also follow `shared/database/AGENTS.md`.

## Do not

- Do not add feature-private persistence internals here.
- Do not introduce a second taxonomy parser or id source of truth.
- Do not make the read mapper strict unless a migration strategy exists.

## Related skills

- `.agents/skills/dictionary-enrichment-schema-review`
- `.agents/skills/sqldelight-schema-aggregation-review`
