# shared/grammar/data

The single runtime source of the grammar/usage/complement taxonomy and its
human-readable labels. Holds the wire DTO, the `practice/grammar/taxonomy`
remote data source, the mock fixture, and `GrammarLabels` (neutral
id -> display-label resolver over a fetched taxonomy).

## Invariants

- Single source of truth: the taxonomy fixture here is the ONLY
  `practice/grammar/taxonomy` `MockFixtureSet` contribution. No other module
  may serve that key (a duplicate collides in `MergedFixtureReader`).
- `GrammarLabels` is pure: built from a `GrammarTaxonomyDto`, no Compose, no
  network, no persistence. It maps neutral `shared/grammar/domain` ids
  (`GrammarTag`, `UsageLabel`, `ComplementType`, `GrammarUnitType`) to labels.
- Labels are learner-facing and in the native language (Russian), consistent
  with the capture model; ids stay the wire/storage contract (canon
  `docs/pos-and-forms.adoc`).
- Both `library` (catalog) and `vocabulary-editor` (capture) may depend on
  this; it sits below features so capture never depends on the downstream
  catalog (ADR-001, EB-1).
