# shared/grammar

Neutral grammar taxonomy shared by every feature that classifies lexical
material. `domain` holds pure value types only: `GrammarTag`,
`GrammarCategory`, `GrammarForm`, `GrammarUnitType`.

## Invariants

- Pure types: no Compose, no DI, no persistence, no other module
  dependencies. It sits below features so both `library` (catalog) and
  `vocabulary-editor` (capture) depend on it without depending on each other —
  capture must never depend on the downstream catalog (ADR-001, EB-1).
- Enum `id` strings are a wire/storage contract (fixtures, DB). Renaming an
  `id` is a data migration, not a rename — change it deliberately, with the
  consumer fixtures/tests in the same task.
