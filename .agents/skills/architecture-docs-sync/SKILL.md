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

- ADR needed: architectural decision, trade-off, invariant, or rejected alternative.
- Arc42 needed: current structure, runtime flow, deployment, quality risk, or concept changed.
- LikeC4 needed: module boundary, relationship, runtime flow, persistence ownership, deployment target, external integration, or documented subsystem changed.
- Scenario needed: user-facing workflow changed or was newly designed.
- README/CONTRIBUTING needed: onboarding, build, verification, release, or setup changed.
- Docs should not duplicate detailed structural relationships already in LikeC4 unless useful for reading.
- Generated diagrams are not committed unless repository convention changes.

## Output

- Docs that may need updates
- LikeC4 model/views that may need updates
- ADR needed: yes/no
- Suggested concise doc patch
- Risk of documentation drift
