# Agent Instructions for `.agents`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for project-specific Agent Skills.

## Scope

Applies to:
- `.agents/...`

## Local context

`.agents/skills` contains portable project-specific Agent Skills. These skills are documentation-like workflow artifacts, not production code and not generated output.

## Local rules

- Every skill folder must contain `SKILL.md`.
- `SKILL.md` frontmatter must include `name` and `description`.
- The `name` must match the folder name and use lowercase letters, numbers, and hyphens only.
- Put triggering guidance in `description`; keep the body focused on workflow and project-specific checks.
- Keep skills concise and apply the staleness-coupling test. If a fact changes when the codebase changes — module inventory, ADR decisions, the LikeC4 model, or canonical policy in `docs/` — link to its source instead of restating it (`docs/`, `docs/adr/`, `docs/c4/*.c4`, `docs/scenarios/README.md`, `docs/adr/index.adoc`). If it is durable, repo-stable methodology — how to review a boundary, author an arc42 chapter, structure an ADR — it may live in the skill, inline or as bundled reference files loaded on demand.
- Add `evals/evals.json` only for high-value skills where seed prompts will help future refinement.
- Do not add placeholder skills. Mark planned/emerging behavior inside a skill only when the repository has a real planned direction for it.

## Local verification

- Validate skill frontmatter (a terminated block with single-line `name`/`description`), folder/name consistency, the `docs/agents/skills.md` catalog ↔ `.agents/skills/` match, structural eval JSON, commit links, thin `CLAUDE.md` shims, and `AGENTS.md`/`CLAUDE.md` sibling pairing:
  ```shell
  python3 scripts/agents/validate-agent-instructions.py
  ```
- The validator does not perform semantic review of skill quality or judge whether eval prompts are good; use manual review and the relevant skills for that.
- If a skill changes commit, docs, LikeC4, or AGENTS behavior, update `docs/agents/skills.md` and root `AGENTS.md` when needed.

## Do not

- Do not create `README.md`, changelogs, or installation guides inside individual skill folders.
- Do not add scripts unless they provide deterministic value and have a safe non-interactive path.
- Do not duplicate or fork the canonical project architecture *state* (module inventory, ADR decisions, the LikeC4 model) inside a skill; link to it. Portable methodology and bundled reference checklists are allowed — see the staleness-coupling test in `## Local rules`.

## Related skills

- `skill-creator` system skill, when available in the running agent.
- `.agents/skills/agent-task-slicer`
- `.agents/skills/pr-diff-review`
- `.agents/skills/commit-preparation`
- `.agents/skills/backlog-maintenance`
