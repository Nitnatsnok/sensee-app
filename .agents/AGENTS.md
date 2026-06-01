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
- Keep skills concise. Link to repository docs, ADRs, LikeC4, or source files instead of copying long reference material.
- Add `evals/evals.json` only for high-value skills where seed prompts will help future refinement.
- Do not add placeholder skills. Mark planned/emerging behavior inside a skill only when the repository has a real planned direction for it.

## Local verification

- Validate skill frontmatter, folder/name consistency, `description` presence, structural eval JSON, stale references, commit links, thin `CLAUDE.md` shims, and `AGENTS.md`/`CLAUDE.md` sibling pairing:
  ```shell
  python3 scripts/agents/validate-agent-instructions.py
  ```
  On Windows:
  ```shell
  py -3 scripts/agents/validate-agent-instructions.py
  ```
- The validator does not perform semantic review of skill quality or judge whether eval prompts are good; use manual review and the relevant skills for that.
- If a skill changes commit, docs, LikeC4, or AGENTS behavior, update `docs/agents/skills.md` and root `AGENTS.md` when needed.

## Do not

- Do not create `README.md`, changelogs, or installation guides inside individual skill folders.
- Do not add scripts unless they provide deterministic value and have a safe non-interactive path.
- Do not duplicate full project architecture docs inside a skill.

## Related skills

- `skill-creator` system skill, when available in the running agent.
- `.agents/skills/agent-task-slicer`
- `.agents/skills/pr-diff-review`
- `.agents/skills/commit-preparation`
