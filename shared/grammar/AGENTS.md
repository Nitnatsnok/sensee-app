# shared/grammar

Neutral grammar taxonomy shared by every feature that classifies lexical
material. Two sibling modules:

- `domain` — pure value types and provider contracts. No DI, no persistence,
  no Compose, no other module dependencies.
- `data` — wire DTOs, the `practice/grammar/taxonomy` JSON fixture (codegen via
  `app.sensee.gradle.mock-fixtures`), and the caching provider implementations.
  Caching providers own only the cache; *when* to preload is the startup
  feature's concern (`PreloadAppStartupUseCase`).

## Invariants — read ADR-006 §2 (forward-compatible domain) and §3 (TaxonomyInvariants) before touching this

- Taxonomy types are `sealed interface`s with a `data class Unknown(id)`
  branch. `fromId(id)` is total: an unknown id surfaces as `Unknown(id)`
  instead of being dropped or crashing exhaustive `when`s.
- Pair invariants (`category → allowedForms`, `axis → allowedValues`,
  `unit_type ∈ known`, `complement ∈ known`) are *runtime*, not
  compile-time. `GrammarTag.resolve` / `UsageLabel.resolve` take an
  optional map: `null` = pass-through, non-null = strict (drops
  off-schema pairs at the AI boundary). Built-in client-known pairs
  live in `GrammarTag.knownAllowedFormsByCategory` for offline fixtures
  and storage.
- `id` strings are a wire/storage contract (fixtures, DB). Adding a new
  built-in id is a fixture extension + a new `data object` in `domain`;
  renaming an existing id is a data migration, not a rename — change it
  deliberately, with the consumer fixtures/tests in the same task.
- Provider contracts are asymmetric on "not loaded" by design:
  `TaxonomyInvariantsProvider.invariants()` returns `null` so the
  AI-boundary mapper switches to pass-through; `GrammarLabelsProvider.labels()`
  returns `GrammarLabels.EMPTY` so a UI lookup falls back to raw ids. Both
  retry on a failed load. The synchronous `cached*()` snapshot mirrors this:
  `null` / `GrammarLabels.EMPTY` before a successful load, the resolved value
  after.
- The splash awaits the preload, so screens normally see a populated snapshot.
  A warm-restore deep-link can land before the preload finishes; in that case
  callers get the degraded values and the UI shows raw ids until the retry
  succeeds.
- Error surfacing is explicit at startup: `PreloadAppStartupUseCase` preloads
  labels and invariants, and `StartupState.Failed` keeps the splash on a
  retryable state.
- Both `library` (catalog) and `vocabulary-editor` (capture) may depend
  on this; it sits below features so capture never depends on the
  downstream catalog (ADR-001, EB-1).
