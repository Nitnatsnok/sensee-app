# shared/lexicon

The neutral lexical-sense model shared by every feature that produces or
consumes confirmed senses. It sits below features, like `shared/grammar`.

- `domain` — `Sense`, the **central model the app is built around** (Sensee),
  plus the neutral lexical read model (`LexicalEntry`, `EntryId`,
  `EntryStatus`, `LexiconRepository`). `Sense` is one confirmed sense of a
  word/phrase with its translation, examples (`ContextualApplication` +
  `AlignmentChunk`), components, grammar, usage labels,
  synonyms/antonyms/collocations and `WordFamilyMember`s. Pure value types and
  read contracts over `grammar.domain`. No DI, no persistence implementation,
  no AI wire, no feature dependency.
- `enrichment` — anti-corruption mapping from provider-agnostic AI enrichment
  (`shared/ai/core`) into central `Sense`. It is shared by Vocabulary Editor
  and Library so service catalog material and user-accepted enrichment converge
  on the same model before persistence/projection.

## Invariant — enrichment serves `Sense`, not the reverse

`Sense` is the target shape. The AI seam (`shared/ai`) is a *producer* that
enriches the user's input into a `Sense`; capture (`vocabulary-editor`) and the
catalog (`library`) both build on the same `Sense`, and the lemma / derivative /
component graph is derived from it. A feature that needs rich lexical material
depends on `lexicon/domain` or `lexicon/enrichment`, never on another feature's
domain for it.
