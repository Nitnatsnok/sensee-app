# Agent Environment

This repository keeps agent setup model-agnostic by default.

## Source of Truth

- `AGENTS.md` is the main contract for all coding agents.
- Nested `AGENTS.md` files refine local rules for their subtree.
- `CLAUDE.md` files stay thin `@AGENTS.md` shims so Claude Code reads the same rules without copied content.
- `.agents/skills` is the canonical project skills location.
- `docs/agents/skills.md` is the short skills catalog.

Tool-specific files should point back to these shared sources instead of copying instructions.

## Script Layout

Shared scripts live in `scripts/agents/`:

- `setup-local-env.sh` / `setup-local-env.ps1` prepare a lightweight local agent worktree.
- `list-tasks.sh` / `list-tasks.ps1` list available Gradle tasks with `tasks --all`.
- `validate.sh` / `validate.ps1` provide common validation entrypoints.
- `validate-agent-instructions.py` validates agent-facing instruction files and skills.

Thin adapters live in:

- `scripts/codex/`
- `scripts/claude/`

The adapters only call the shared setup scripts.

## Setup Behavior

`scripts/agents/setup-local-env.*` is intentionally lightweight:

- prints the current project directory;
- prints Java and Gradle wrapper versions;
- makes `./gradlew` executable on Unix;
- writes ignored `local.properties` when `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to an Android SDK;
- runs `./gradlew --no-daemon help`.

It does not run full `check`, emulators, desktop/web app launches, screenshot capture, device checks, or UI recordings.

## Validation Modes

Use `scripts/agents/validate.*` with one of these modes:

- `fast` runs Gradle `help` and the agent-instruction validator.
- `code` runs the best available aggregate code validation task: `verify`, then `check`.
- `docs` runs the agent-instruction validator and `verifyDocs` if such a Gradle task exists.
- `architecture` runs `verifyArchitecture`, then `konsistCheck`, if available.

The scripts inspect Gradle tasks before running optional aggregate tasks. Missing optional tasks are reported instead of treated as repository failures.

For module-local code changes, prefer the smallest affected Gradle task from `AGENTS.md` over a broad aggregate script mode.

If Python is not discoverable as `python3`, `python`, or `py -3`, set `PYTHON` to the interpreter that should run `scripts/agents/validate-agent-instructions.py`.

## Config Policy

- Codex Local Environment config is committed at `.codex/environments/environment.toml`.
- Future app-generated `.codex` changes must be reviewed before committing.
- Claude Code project config is committed at `.claude/settings.json` and is limited to safety-oriented `permissions.deny` rules.
- Do not create committed Claude hooks by default; hooks require a separate deliberate decision and documentation.
- Keep Claude-specific repository behavior in `docs/agents/claude-code.md` and the thin `CLAUDE.md` shim.
- Keep Codex-specific setup notes in `docs/agents/codex-local-environment.md`.
- Do not copy `.agents/skills` into `.claude/skills` or another tool-specific tree.

Local-only preferences may live in ignored files such as `CLAUDE.local.md` or `.claude/settings.local.json`.

## Aggregate Task Decision

The repository currently has root `check` and `konsistCheck` tasks. It does not have `verify`, `verifyDocs`, or `verifyArchitecture`.

`scripts/agents/validate.*` therefore uses available tasks and reports missing optional aggregates. New aggregate Gradle tasks are future deliberate build-logic work, not part of this docs/scripts-only environment layer.

## References

- OpenAI Codex Local Environments: https://developers.openai.com/codex/app/local-environments
- OpenAI Codex Worktrees: https://developers.openai.com/codex/app/worktrees
- OpenAI Codex `AGENTS.md`: https://developers.openai.com/codex/guides/agents-md
- Claude Code memory and `AGENTS.md` import: https://code.claude.com/docs/en/memory
- Claude Code settings: https://code.claude.com/docs/en/settings
- Claude Code hooks: https://code.claude.com/docs/en/hooks-guide
- Claude Code skills: https://code.claude.com/docs/en/skills
- Agent Skills specification: https://agentskills.io/specification
- `AGENTS.md` guide: https://agents.md/
