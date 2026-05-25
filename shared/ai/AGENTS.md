# shared/ai

The AI enrichment seam. `core` holds the provider-agnostic and
feature-agnostic boundary: request/result types, the versioned wire DTO, the
`AiEnrichmentClient` interface, and the `EnrichmentResponseMapper`
anti-corruption mapping. Provider and fixture implementations live in sibling
modules and depend on `core`, never the reverse.

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
