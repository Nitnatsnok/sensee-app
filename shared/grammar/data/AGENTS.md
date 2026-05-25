# shared/grammar/data

Runtime data source for the grammar/usage/complement taxonomy. This module owns
the wire DTO, the `practice/grammar/taxonomy` mock fixture, the remote/source
adapter, and cached projections into `GrammarLabels` and `TaxonomyInvariants`.
The broader taxonomy semantics live in the parent `shared/grammar/AGENTS.md`;
this file only adds data-module rules.

## Invariants

- Single source of truth: the taxonomy fixture here is the ONLY
  `practice/grammar/taxonomy` `MockFixtureSet` contribution. No other module
  may serve that key (a duplicate collides in `MergedFixtureReader`).
- Projection builders are pure: `GrammarTaxonomyDto.toGrammarLabels()` and
  `toTaxonomyInvariants()` have no Compose, no persistence, and no DI side
  effects. Caching belongs in `CachingGrammarTaxonomyProvider` and its two
  projection providers.
- Labels are multi-language: the taxonomy carries one `{long, short}` pair per
  id per BCP-47 language tag (e.g. `ru` for the learner's native language,
  `en` for the study language). The same resolver serves both the detail
  panel (native, long) and the sense-card badges (study, short); callers pick
  language + form per lookup. Adding a new UI language is a fixture
  extension here (add the tag under `labels` everywhere in
  `src/commonMain/mockFixtures/practice/grammar/taxonomy.json`) — not a code
  change in `domain`. Ids stay the wire/storage contract (canon
  `docs/pos-and-forms.adoc`).
- The `MockFixtureSet` class is generated from the JSON file by the
  `app.sensee.gradle.mock-fixtures` plugin; edit the JSON, not the generated
  Kotlin under `build/generated/`.
