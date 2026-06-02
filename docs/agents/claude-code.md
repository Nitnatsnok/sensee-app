# Claude Code

Use this page to run Claude Code in this repository without creating a parallel instruction system.

## Instruction Source

Claude Code reads `CLAUDE.md`, not `AGENTS.md`. The repository root and nested instruction directories therefore keep `CLAUDE.md` as:

```md
@AGENTS.md
```

In Claude-compatible environments that support file references, this imports the matching `AGENTS.md` and keeps one source of rules without copied content. It is the supported shim pattern for those environments, not a guarantee for every possible agent runtime. The repository validator enforces that each `CLAUDE.md` stays this thin `@AGENTS.md` shim and has a sibling `AGENTS.md`.

Claude-specific operational notes belong in this document, not in copied instruction blocks.

## Setup

Run the shared local setup (cross-platform, needs `python3` on `PATH`):

```shell
python3 scripts/agents/setup-local-env.py
```

## Skills

Project skills live in `.agents/skills`, not `.claude/skills`. The catalog is `docs/agents/skills.md`.

Claude Code supports project skills under `.claude/skills`, but this repository intentionally avoids copying the canonical skills there. If Claude Code needs help discovering a workflow, point it to the relevant `.agents/skills/<skill-name>/SKILL.md` or the catalog.

For native discovery (auto-trigger and `/skill-name` invocation), you can opt in to a local, git-ignored link from `.claude/skills` to the canonical `.agents/skills`. This links rather than copies, so the single source of truth stays in `.agents/skills`:

```shell
python3 scripts/agents/link-claude-skills.py
```

The script creates a relative symlink on Unix and a directory junction on Windows (no admin rights or Developer Mode required). `.claude/skills` is git-ignored, so the link is per-machine and never committed. Pass `--remove` to drop it.

Create a new skill only when `.agents/skills` does not already cover the workflow.

If future Claude-specific discovery needs adapters, use thin adapters or docs that point to `.agents/skills`; do not copy full skill bodies into `.claude/skills`.

## Worktrees

Claude Code can run isolated work in git worktrees (the `--worktree` flag, `isolation: worktree` subagents, and background-session isolation). Three mechanisms keep the skills link working across worktrees and fresh clones.

`worktree.baseRef: "head"` (committed `.claude/settings.json`) branches new worktrees from local `HEAD` instead of `origin/<default-branch>`. This repository is local-first and `origin/main` is an empty initial commit, so `"fresh"` (the default) would produce worktrees without the codebase. This is committed because it is correct for everyone working against this empty origin, not a per-machine preference.

A committed `SessionStart` hook runs `python3 scripts/agents/link-claude-skills.py` on session startup, so a fresh session in any checkout (clone or worktree) recreates `.claude/skills` pointing at that checkout's own `.agents/skills`. The script creates a relative symlink on Unix and a directory junction on Windows, so the result is always a real link, never a copy. The hook runs in the shell Claude Code uses for hooks, so `python3` must be available there. It also relies on the session starting at the repository root (the hook's working directory). This is a deliberate, documented exception to the "no committed hooks" default; it only ensures the skills link and never edits tracked files.

`.worktreeinclude` (repo root) covers the one path the hook does not: an in-session `EnterWorktree`, which switches directory without a new session start. It lists git-ignored paths that Claude Code copies from the main checkout into each new worktree, using `.gitignore` syntax (only already-ignored paths are copied, so tracked files are never duplicated). Sensee lists:

```text
.claude/settings.local.json
.claude/skills
```

On Windows the harness dereferences the `.claude/skills` junction and copies the skill files as a plain directory taken at creation time, so for a long-lived worktree on a branch with different skills, re-run `python3 scripts/agents/link-claude-skills.py` inside it to repoint at that worktree's own `.agents/skills`. The script detects a harness-copied skill tree (a directory whose entries are all skill folders, each holding `SKILL.md`, or an empty directory) and replaces that copy with the link automatically — no manual deletion needed, because the canonical skills still live in `.agents/skills`. It still refuses to overwrite a `.claude/skills` that holds anything else.

`worktree.bgIsolation` stays out of the committed settings; it is a safety toggle. The default (isolation on) keeps background sessions in their own worktree. A developer who wants a background session to edit the main checkout directly sets `worktree.bgIsolation: "none"` in their own `.claude/settings.local.json`. Because `.claude/settings.local.json` overrides `.claude/settings.json`, per-machine values always win over the committed ones.

## Settings and Hooks

Claude Code supports project settings in `.claude/settings.json`, local settings in `.claude/settings.local.json`, hooks, and permissions.

Settings precedence (highest to lowest): managed policy, command-line flags, `.claude/settings.local.json`, `.claude/settings.json`, `~/.claude/settings.json`. So a per-machine `.claude/settings.local.json` value overrides the committed `.claude/settings.json`. `permissions` rules are the exception — they merge across scopes instead of overriding.

The committed `.claude/settings.json` is kept minimal and contains only:

- `$schema` for editor validation against the official Claude Code settings schema.
- `permissions.deny` rules that block reads of local environment files, secrets directories, nested `secrets.properties`, keystores, certificate/private-key files, and nested `local.properties`.
- `worktree.baseRef: "head"`, required for worktrees to carry the codebase against this repository's empty `origin/main` (see Worktrees).
- a single `SessionStart` hook that recreates the per-machine `.claude/skills` link (see Worktrees). This is the one committed hook; adding any other committed hook is a separate deliberate decision.

It intentionally still does not contain:

- `permissions.allow` rules;
- telemetry or environment settings;
- copied `AGENTS.md` content;
- skill bodies.

Use local-only settings for per-machine preferences (for example `worktree.bgIsolation`):

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

Common commands (cross-platform):

```shell
python3 scripts/agents/validate.py fast
python3 scripts/agents/validate.py docs
python3 scripts/agents/list-tasks.py
```

For code changes, prefer the smallest affected Gradle task from `AGENTS.md`. Use `validate full` only when a full project check is appropriate.

Current aggregate task decision:

- `check` exists and is the full project verification task (`validate full`).
- `konsistCheck` exists and is the architecture check (`validate architecture`).
- `validate.py` modes map directly to these real tasks; `verify`, `verifyDocs`, and `verifyArchitecture` do not exist and are not assumed.

## References

- Claude Code memory and `AGENTS.md` import: https://code.claude.com/docs/en/memory
- Claude Code settings: https://code.claude.com/docs/en/settings
- Claude Code hooks: https://code.claude.com/docs/en/hooks-guide
- Claude Code skills: https://code.claude.com/docs/en/skills
- Agent Skills specification: https://agentskills.io/specification
