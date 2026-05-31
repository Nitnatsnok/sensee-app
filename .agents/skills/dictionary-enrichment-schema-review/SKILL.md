---
name: dictionary-enrichment-schema-review
description: Use this skill when Sensee work touches vocabulary enrichment prompts or schemas, AI enrichment DTOs, lexical verification evidence, dictionary/provider adapters, part-of-speech/forms modeling, phrase/idiom/phrasal-verb handling, sense splitting, or material verification flows.
---

# Dictionary Enrichment Schema Review

Status: active.

## Operating mode

Review/support. Check schema and evidence boundaries before recommending prompt, DTO, or provider changes.

## Non-goals

- Do not merge AI generation, dictionary verification, grammar taxonomy, and persistence into one boundary.
- Do not treat provider suggestions as canonical user material.

## Workflow

1. Read relevant local instructions: `shared/ai/AGENTS.md`, `shared/verification/AGENTS.md`, `shared/grammar/AGENTS.md`, and `shared/lexicon/AGENTS.md`.
2. Identify whether the change belongs to AI generation, verification evidence, grammar taxonomy, central `Sense`, feature candidate state, or persistence DTOs.
3. Check schema versioning, validation, availability, and fallback behavior.
4. Recommend tests for edge cases before broad prompt or schema changes.

## Check

- One returned item represents one distinct sense.
- Phrasal verbs, idioms, fixed phrases, and single words are not collapsed into one generic meaning.
- `shared/ai/core` remains provider-agnostic and feature-agnostic.
- The AI response schema remains versioned and mapped through anti-corruption code.
- Prompt text does not duplicate per-field schema semantics unnecessarily.
- AI output validation is explicit.
- Dictionary/API verification is separated from AI generation.
- License flags from verification sources are respected before using or storing evidence.
- Unknown grammar/taxonomy ids remain forward-compatible.
- Fallback behavior is clear when verification is unavailable or inconclusive.
- LikeC4/docs are updated for significant AI/enrichment/verification boundary changes.

## Output

- Schema/prompt invariant impact
- Sense-splitting risks
- Verification/fallback gaps
- LikeC4/docs impact
- Recommended minimal fix
- Test cases to add
