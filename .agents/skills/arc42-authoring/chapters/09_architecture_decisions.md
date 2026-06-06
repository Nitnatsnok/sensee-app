# Chapter 9 — Architecture Decisions (Архитектурные решения)

## Purpose
Record important, expensive, large-scale, or risky architecture decisions with their rationale — a decision chooses one option against given criteria. Acts as the central index of decisions.

## Required / recommended subsections
- Use ADRs (one decision per record) or an ordered list/table. In Sensee this is the index in `docs/adr/index.adoc` (write/review individual ADRs via `adr-authoring`); §9 links to that index instead of holding its own table.
- **Avoid redundancy**: many decisions are already summarized in §4 — this section refers to §4 rather than repeating it, and it does not copy ADR bodies into arc42.

## Form
An index/table linking to ADRs; per-decision rationale lives in the ADR, not here.

## Do not put here
- The short strategy summary (→ §4). Full ADR bodies (→ `docs/adr/`). Structure/relationships (→ §5 / LikeC4).

## Related chapters
§4 (Solution Strategy — referenced, not duplicated), §10 (decision drivers), §5 (resulting structure).

## Source
arc42 §9 — https://docs.arc42.org/section-9/
