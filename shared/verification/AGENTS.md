# shared/verification

> Rationale lives in `README.md`. This file is the technical reference.

The lexical-verification seam. Same convention as `shared/ai` and `shared/tts`:
`core` is the provider-agnostic boundary contract (no dictionary SDK, no
feature domain types); sibling modules are implementations (`sensee-curated`
for our reference data served over HTTP, network adapters for LanguageTool,
Datamuse and Free Dictionary); `integration` composes them into the single
bound `LexicalVerifier` the app depends on (`PersistedCachingLexicalVerifier`
→ `RoutingLexicalVerifier` → providers).

The seam answers structured questions about a lexical unit — does it exist,
what is its headword/part-of-speech inventory, what is its CEFR level and
frequency band, is the example sentence natural, and what is the unit's family
(siblings, head lemma) — and returns the answers as evidence with confidence
plus optional findings. The orchestrator (a feature use case) decides whether
to apply suggested fixes; the seam never silently rewrites material.

## Invariants — same shape as ADR-005 for the AI seam

- `core` depends on no dictionary SDK and no feature domain type. Mirror the
  neutral-string approach the AI seam uses: [`SentenceHint`],
  [`UnitComponentFact`], [`PartOfSpeechHint`] etc. resolve back into feature
  taxonomy at the consuming feature's boundary.
- Availability is first-class. `Unavailable`/`Degraded` are normal results the
  consuming feature renders or routes around — never throw across the seam,
  never map a malformed provider response straight into feature domain.
- The seam never silently overwrites. It emits `Finding`s with severity and
  evidence, plus optional `SuggestedAction`s. Whether to apply a suggested
  rewrite or replace a sense lives in the orchestrator.
- Sources carry their own [`LicensePolicy`]. Two policy flags are load-bearing
  and must be respected by callers: `usableAsLlmContext` (may a snippet be
  sent to a third-party LLM as evidence) and `storeContentAllowed` (may a
  response be persisted to the on-device L2 cache). The default is the most
  restrictive (`false`/`false`), explicit opt-in per adapter.
- Multi-word units (phrasal verbs, idioms, fixed phrases) are first-class. The
  query type carries `expectedEntryType` and the report carries family
  evidence so a single-word lookup never gets silently mapped onto a phrasal.
- One adapter per source. LanguageTool, Datamuse, Free Dictionary and future
  Cambridge/Merriam-Webster-style HTTP sources each own their adapter; our own
  reference facets (frequency, CEFR, sense inventory, family) are served by the
  `sensee-curated` adapters over the backend, not by separate per-source
  modules.

## Source kinds — our backend + third-party network

The seam draws lexical evidence from two kinds of source. There is no
on-device prebuilt database and no bundled third-party corpus: everything is
fetched over HTTP at verification time.

**Sensee-curated reference — our data, served by the backend.** Hand-authored
frequency / CEFR / sense / family data. In this build it is served by the
**mock backend** (`shared/core/mock-backend`): one editable JSON catalog per
facet under `shared/verification/sensee-curated/src/commonMain/mockFixtures/
verification/` — `frequency` (lemma → zipf), `cefr` (lemma → level), `senses`
(lemma → glosses), `family` (head lemma → units). The `sensee-curated`
adapters (`SenseeFrequencyProvider`, `SenseeCefrLevelProvider`,
`SenseeSenseInventoryProvider`, `SenseeLexicalFamilyProvider`) fetch a catalog
once, cache it, and map it into the `core` result types. A real Sensee backend
will serve the same shapes under the same `SenseeCuratedSource` identity — the
client does not change. Licence-clean (`storeContentAllowed`, `usableAsLlmContext`
both `true`).

**Third-party network — runtime API.** `FreeDictionaryLexicalAdapter`
(existence/POS/pronunciation), `DatamuseLexicalAdapter` (spelling
normalisation), `LanguageToolExampleChecker` (example quality). HTTP calls at
verification time are gated by the request-level `policy.allowNetwork`; there
is no user-facing client setting for this. Each adapter owns its own
`HttpClient` so an outage in one cannot starve the others. Responses are cached
by `PersistedCachingLexicalVerifier` (ADR-009), L2 persistence license-gated by
`storeContentAllowed`.

Coverage by source:

|Source|Existence/POS|Frequency|CEFR|Senses|Family|Example quality|
| --- | --- | --- | --- | --- | --- | --- |
|`sensee-curated` (backend)| — |✔|✔|✔|✔| — |
|`free-dictionary` (network)|✔| — | — | — | — | — |
|`datamuse` (network)|✔ (spell-fix)| — | — | — | — | — |
|`languagetool` (network)| — | — | — | — | — |✔|

When adding a new source, decide its kind first:

- Our own reference data → add the JSON to `sensee-curated`'s mock fixtures
  (one catalog per facet); extend the matching adapter only if the facet's
  shape changes. See `AUTHORING.md`.
- A third-party HTTP API → new module under `shared/verification/<name>/`,
  one adapter implementing the appropriate `core` contract, its own
  `HttpClient`, and a `*Source.descriptor` with the licence flags the API
  actually permits. Register the descriptor in the catalog (below).

> No on-device prebuilt database and no bundled third-party corpus: reference
> data is backend-served, which makes the seam work uniformly on every target
> (including web/iOS) and keeps the repo free of vendor-licensed datasets.

Cross-references: ADR-007 (seam + licence aggregator), ADR-009 (cache
two-layer + `storeContentAllowed`), `AUTHORING.md` (how to edit the curated
fixtures).

## Persistence — verifier cache

`PersistedCachingLexicalVerifier` is the bound `LexicalVerifier`. Two
layers:

1. L1 — in-memory LRU (`MAX_MEMORY_ENTRIES`, default 256) for the active
   capture session. Touched on every hit.
2. L2 — SQLDelight-backed (`shared/verification/database-schema` →
   `lexical_verification_cache`) so cold-launch lookups still hit. The
   table carries `expires_at_epoch_ms` and `last_accessed_at_epoch_ms`
   columns; `selectOldestKeys` drives capped eviction, `deleteExpired`
   runs once per process at first verify, and `touchAccess` is throttled
   to a per-minute granularity to avoid hot-key write storms.

Persistence is license-gated: an entry reaches L2 only when **every**
source in `report.sources` has `storeContentAllowed = true`. Strict
commercial-license sources ride only the in-memory layer.

`TTL` per cache entry is `min(every source's maxCacheTtlMillis,
policy.maxCacheAgeMillis, DEFAULT_TTL_MILLIS)`. Queries that carry
`examplesToValidate` bypass the cache entirely — example findings are
sentence-specific, so the lemma-keyed map would otherwise return stale
findings for new sentences.

## DI & source catalog

Adapters wire up through `VerificationContributors`, which Metro injects
into `RoutingLexicalVerifier`. Each contributor type (`Set<X>`) is
populated in `VerificationIntegrationProviders.provideX(...)` — explicit
`setOf(...)` aggregation matches the project-wide convention. The
`sensee-curated` adapters share one `SenseeCuratedCatalogs` (the cached HTTP
fetcher, injected with the app's `HttpClient`); the network adapters each get
their own short-lived `HttpClient` so a dictionary outage stays isolated.

The `Set<LexicalSource>` catalog is the authoritative list of every source
descriptor (`SenseeCuratedSource`, `FreeDictionarySource`, `DatamuseSource`,
`LanguageToolSource`); the aggregator pulls licence + attribution metadata
from it when stitching the merged report, and treats an *uncatalogued*
contributing source as non-persistable (fail-closed). **The catalog is
hand-maintained**: `SourceCatalogCompletenessTest` asserts it matches the
expected set of `*Source.ID` constants — add to that test whenever a new
adapter module joins.

## sensee-curated module

`shared/verification/sensee-curated` is the runtime home of our reference
data. It holds the four HTTP adapters, the `SenseeCuratedCatalogs` fetcher,
the wire DTOs, the `SenseeCuratedSource` descriptor, and the editable catalog
fixtures under `src/commonMain/mockFixtures/verification/`. It applies
`sensee.kmpLibrary` + `sensee.mockFixtures` + serialization + `metro`, and
depends only on `:shared:verification:core` and `:shared:core:mock-backend`.
Network adapters and the umbrella `integration` module keep their own
hand-written `build.gradle.kts`.
