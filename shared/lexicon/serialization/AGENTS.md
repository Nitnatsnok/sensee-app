# shared/lexicon/serialization

The **shared** serialized shape of [Sense] and its bidirectional mapping to the
domain type. A neutral, non-feature module that sits below features (sibling to
`lexicon/domain`). It holds ONLY shared DTOs — `SenseDto` + its sub-DTOs +
`Sense.toDto()` / `SenseDto.toDomain()` — nothing feature-private.

It deliberately lives in a non-`data` package (`app.sensee.lexicon.serialization`)
so feature data layers may depend on it: the sealed-data konsist rule
(`FeatureLayeringKonsistTest`) forbids a feature data layer from importing any
`*.data.*` package — that rule is for feature-private persistence internals, and
it is correct as written. A shared serializable boundary contract is a different
thing and belongs in a non-`data` package — the same reason the shared wire DTO
`EnrichmentItemV1` lives in `ai.core`, not `ai.data`.

- `SenseDto` mirrors every `Sense` field as serializable ids/markers; the mappers
  re-resolve through the same neutral `parse`/`fromId` the AI boundary uses — no
  second source of truth. On read the mapper is **pass-through** (an id outside
  the sealed set surfaces as `Unknown(id)`, never dropped): values were written
  through the strict AI-boundary mapper, so they are trusted, and pass-through
  keeps stored data forward-compatible across taxonomy growth.

## Invariant — `Sense` is what gets persisted, enrichment is the producer format

Both capture (`vocabulary-editor`) and the catalog (`library`) store a `Sense`
via this DTO. Enrichment (`shared/ai`) is the *producer/wire* format on both
paths: capture maps an AI candidate → `Sense` at confirm, the catalog maps an
ideal-enrichment item → `Sense` at sync; from then on the persisted shape is the
same `SenseDto`. A user-captured sense and a service-deck sense are therefore the
same stored thing — which is the point (when the backend lands, the two must be
identical).
