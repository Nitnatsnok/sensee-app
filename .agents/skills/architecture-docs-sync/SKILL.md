---
name: architecture-docs-sync
description: Use this skill when Sensee work may require docs, ADR, scenario, README/CONTRIBUTING, or LikeC4 updates because architecture, module boundaries, runtime behavior, CI/build workflow, persistence, navigation, observability, public contracts, or implemented/planned state changed.
---

# Architecture Docs Sync

Status: active.

## Operating mode

Review/support. Decide documentation impact first; edit docs only when the current task asks for changes.

## Non-goals

- Do not move architecture decisions out of ADRs or structural relationships out of LikeC4.
- Do not update docs for trivial local code changes that do not affect documented behavior or boundaries.

## Workflow

1. Read the closest `AGENTS.md`; for docs, follow `docs/AGENTS.md`.
2. Compare the change with:
   - `docs/README.md`;
   - relevant `docs/arc42/sections/*`;
   - `docs/adr/index.adoc` and specific ADRs;
   - `docs/c4/*.c4`;
   - `docs/scenarios/*.feature` for user-facing behavior.
3. Decide whether docs, ADR, LikeC4, scenarios, or README need updates.
4. Keep docs factual: implemented first, planned/experimental marked explicitly.

## Check

- ADR needed: architecturally significant decision, trade-off, cross-module contract, or rejected alternative (a local invariant belongs in a test or KDoc, not an ADR). Author/review the ADR via `adr-authoring`.
- Arc42 needed: current structure, runtime flow, deployment, quality risk, or concept changed; author/review the chapter via `arc42-authoring`.
- LikeC4 needed: module boundary, relationship, runtime flow, persistence ownership, deployment target, external integration, or documented subsystem changed.
- Scenario needed: user-facing workflow changed or was newly designed; author/review via `gherkin-scenario-authoring`.
- Prose language: a focused language/terminology pass uses `docs-language-review`.
- README/CONTRIBUTING needed: onboarding, build, verification, release, or setup changed.
- Docs should not duplicate detailed structural relationships already in LikeC4 unless useful for reading.
- Generated diagrams are not committed unless repository convention changes.
- Early-stage relaxation: if the change deliberately relaxes a documented rule for speed, mark it `early-stage` and register it in arc42 §11 with a tightening trigger (`backlog-maintenance` for an actionable `EB-N`).

## Output

- Docs that may need updates
- LikeC4 model/views that may need updates
- ADR needed: yes/no
- Suggested concise doc patch
- Risk of documentation drift
