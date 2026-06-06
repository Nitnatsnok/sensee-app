# Chapter 6 — Runtime View (Представление времени выполнения)

## Purpose
Describe concrete behavior and interaction of building blocks as scenarios: important use cases/features, critical external interfaces, operation and administration, error and exception handling. The selection criterion is architectural relevance.

## Required / recommended subsections
- One subsection per runtime scenario, each describing the interaction and its important aspects.
- This is a **representative selection** — not exhaustive, and not a 1:1 mapping to any other artifact.

## Form
Sequence/activity diagrams, state machines, numbered step lists, or prose — whatever communicates the interaction.

## Do not put here
- Static structure (→ §5). Deployment topology (→ §7).

## Related chapters
§5 (the blocks that interact), §7 (Deployment). In Sensee, runtime scenarios are pinned behaviorally as Gherkin in `docs/scenarios/` (see `gherkin-scenario-authoring`) and as `*_flow` views in `docs/c4/`.

## Source
arc42 §6 — https://docs.arc42.org/section-6/
