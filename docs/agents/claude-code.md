# Claude Code

Use this page to run Claude Code in this repository without creating a parallel instruction system.

## Instruction Source

Claude Code reads `CLAUDE.md`, not `AGENTS.md`. The repository root and nested instruction directories therefore keep `CLAUDE.md` as:

```md
@AGENTS.md
```

This imports the matching `AGENTS.md` and keeps one source of rules. The repository validator enforces this thin shim.

Claude-specific operational notes belong in this document, not in copied instruction blocks.

## Setup

Run the shared local setup through the Claude wrapper:

```shell
sh scripts/claude/setup-local-env.sh
```

On Windows:

```powershell
pwsh -File scripts/claude/setup-local-env.ps1
```

The wrapper delegates to `scripts/agents/setup-local-env.*`.

## Skills

Project skills live in `.agents/skills`, not `.claude/skills`. The catalog is `docs/agents/skills.md`.

Claude Code supports project skills under `.claude/skills`, but this repository intentionally avoids copying the canonical skills there. If Claude Code needs help discovering a workflow, point it to the relevant `.agents/skills/<skill-name>/SKILL.md` or the catalog.

Create a new skill only when `.agents/skills` does not already cover the workflow.

If future Claude-specific discovery needs adapters, use thin adapters or docs that point to `.agents/skills`; do not copy full skill bodies into `.claude/skills`.

## Settings and Hooks

Claude Code supports project settings in `.claude/settings.json`, local settings in `.claude/settings.local.json`, hooks, and permissions.

Current project policy: Sensee commits a minimal `.claude/settings.json` safety configuration and does not commit hooks.

The project settings file contains only:

- `$schema` for editor validation against the official Claude Code settings schema.
- `permissions.deny` rules that block reads of local environment files, secrets directories, keystores, and `local.properties`.

It intentionally does not contain:

- `permissions.allow` rules;
- hooks;
- telemetry or environment settings;
- copied `AGENTS.md` content;
- skill configuration.

Use local-only settings when needed:

```text
.claude/settings.local.json
```

`.claude/settings.local.json` and `CLAUDE.local.md` stay ignored because they are per-machine/per-user configuration.

Hooks policy:

- No committed hooks by default.
- Hooks are lifecycle automation that can run deterministic shell, prompt, agent, or HTTP actions at Claude Code events.
- Sensee currently uses `AGENTS.md`, `.claude/settings.json` deny rules, and shared scripts instead of hidden hooks.
- Any future committed hook requires a separate deliberate decision, official schema review, and documentation here.

## Validation

Common commands:

```shell
sh scripts/agents/validate.sh fast
sh scripts/agents/validate.sh docs
sh scripts/agents/list-tasks.sh
```

Windows:

```powershell
pwsh -File scripts/agents/validate.ps1 -Mode fast
pwsh -File scripts/agents/validate.ps1 -Mode docs
pwsh -File scripts/agents/list-tasks.ps1
```

For code changes, prefer the smallest affected Gradle task from `AGENTS.md`. Use `validate code` only when an aggregate code check is appropriate.

Current aggregate task decision:

- `check` exists and remains the full project verification task.
- `konsistCheck` exists and remains the architecture check.
- `verify`, `verifyDocs`, and `verifyArchitecture` do not exist yet.
- `scripts/agents/validate.*` checks optional tasks before running them and does not fail merely because optional docs/architecture aggregates are absent.

## References

- Claude Code memory and `AGENTS.md` import: https://code.claude.com/docs/en/memory
- Claude Code settings: https://code.claude.com/docs/en/settings
- Claude Code hooks: https://code.claude.com/docs/en/hooks-guide
- Claude Code skills: https://code.claude.com/docs/en/skills
- Agent Skills specification: https://agentskills.io/specification
