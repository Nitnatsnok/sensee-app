---
name: arc42-authoring
description: Use when authoring or reviewing an arc42 architecture chapter (docs/arc42/sections/*) or the document's chapter structure — check it against the arc42 standard's purpose, required subsections, and form via the bundled chapters/NN reference. Not for deciding documentation impact (architecture-docs-sync), the LikeC4 model (likec4-architecture-model-review), ADR content (adr-authoring), or prose language (docs-language-review).
---

# Arc42 Authoring

Status: active.

## Operating mode

Review/support. Help author or review an arc42 chapter against the standard; edit chapters only when the current task asks. Format-agnostic: work with the chapter's current format (AsciiDoc here) and do not reformat unrelated text.

## Non-goals

- Do not duplicate Sensee-specific architecture state — module inventory, ADR decisions, the LikeC4 model. Those live in `docs/adr/`, `docs/c4/`, `docs/domain/` and are linked, not restated.
- Do not invent chapters or deep levels for symmetry; prefer relevance over completeness.
- Do not present planned work as implemented.

## Workflow

1. Identify which arc42 chapter(s) the change touches.
2. Open the matching `chapters/NN_*.md` reference for each and check the chapter against its purpose, required subsections, and form.
3. Mark implemented vs prepared/planned explicitly (Sensee convention).
4. To synchronize structure/decisions/scenarios with the rest of the docs, delegate by skill discovery: `architecture-docs-sync` (overall doc impact), `likec4-architecture-model-review` (model), `adr-authoring` (decisions), `gherkin-scenario-authoring` (runtime scenarios), `docs-language-review` (prose).

## Chapter reference map

| # | Chapter | Reference |
| --- | --- | --- |
| 1 | Introduction and Goals | `chapters/01_introduction_and_goals.md` |
| 2 | Architecture Constraints | `chapters/02_architecture_constraints.md` |
| 3 | Context and Scope | `chapters/03_context_and_scope.md` |
| 4 | Solution Strategy | `chapters/04_solution_strategy.md` |
| 5 | Building Block View | `chapters/05_building_block_view.md` |
| 6 | Runtime View | `chapters/06_runtime_view.md` |
| 7 | Deployment View | `chapters/07_deployment_view.md` |
| 8 | Crosscutting Concepts | `chapters/08_concepts.md` |
| 9 | Architecture Decisions | `chapters/09_architecture_decisions.md` |
| 10 | Quality Requirements | `chapters/10_quality_requirements.md` |
| 11 | Risks and Technical Debt | `chapters/11_technical_risks.md` |
| 12 | Glossary | `chapters/12_glossary.md` |

## Check

- Each touched chapter covers its arc42 purpose and required subsections (see its reference file).
- Content sits in the right chapter: decisions → §9 (referencing §4, not duplicating it); runtime interaction → §6; deployment mapping → §7; constraints → §2.
- No duplication between chapters; indexes and relationships link to their canonical source instead of being restated.
- Implemented vs prepared/planned is marked.
- Cross-references between chapters and to `docs/adr/`, `docs/c4/`, `docs/domain/` are valid.

## Output

- Touched chapters and gaps versus the arc42 standard
- Concrete subsection/structure fixes
- What to synchronize (model / ADR / scenarios) and via which skill
