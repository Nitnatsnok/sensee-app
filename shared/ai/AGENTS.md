# shared/ai

The AI enrichment seam. `core` holds the provider-agnostic and
feature-agnostic boundary: request/result types, the versioned wire DTO, the
`AiEnrichmentClient` interface, and the `EnrichmentResponseMapper`
anti-corruption mapping. Provider implementations (curated, LLM) live in
sibling modules and depend on `core`, never the reverse.

## Invariants — read ADR-005 before touching this

- `core` depends on no AI vendor SDK and no feature domain type. Keep it
  that way: leaking either breaks the boundary ADR-005 exists to enforce.
- Availability is first-class. `Unavailable`/`Degraded` are normal results the
  consuming feature can render or recover from — never throw across the seam,
  never map a malformed provider response straight into feature domain.
- Output is always a candidate (ADR-001). The seam never produces canonical
  content; consumers map suggestions into their own candidate model and require
  explicit user confirmation.
- The wire DTO is versioned. Schema changes are deliberate, mapped migrations
  with a `commonTest` case, not silent edits.

## Routing — curated wins, LLM fills the tail

The bound `AiEnrichmentClient` is `RoutingAiEnrichmentClient`, two layers
in order:

1. `CuratedAiEnrichmentClient` — hand-authored top-N coverage
   (`shared/ai/curated-enrichment`). On a non-empty result, returns
   immediately; the LLM is not consulted.
2. `LlmAiEnrichmentClient` — used when an API key is set in settings.

On no-key + curated-miss the router returns `Unavailable` and the wizard
degrades to manual.

The curated layer is consulted only for an unsteered default-language request
(no manual note, `SenseCoverage.Common`, no preferred topics). A key holder who
set topic preferences routes straight to the LLM — curated examples are not
topic-steered.

`CuratedAiEnrichmentClient` is served by the (mock) backend over HTTP:
`GET enrichment/{lemma-slug}` returns a wire `EnrichmentResponseV1` for a
covered lemma, `404` for a miss (the router then falls through to the LLM).
Coverage lives as one editable JSON fixture per lemma under
`shared/ai/curated-enrichment/src/commonMain/mockFixtures/enrichment/`; add or
edit a lemma by adding/editing a file. The real backend will serve the same
shape under the same client. Same delivery model as the verification side's
`sensee-curated` reference data.
