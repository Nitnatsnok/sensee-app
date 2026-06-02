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

Shared scripts live in `scripts/agents/` as cross-platform Python, invoked as `python3 scripts/agents/<name>.py` on every platform:

- `setup-local-env.py` prepares a lightweight local agent worktree.
- `list-tasks.py` lists available Gradle tasks with `tasks --all`.
- `validate.py` provides common validation entrypoints.
- `validate-agent-instructions.py` validates agent-facing instruction files and skills.
- `link-claude-skills.py` creates a local, git-ignored link from `.claude/skills` to the canonical `.agents/skills` so Claude Code can discover the skills natively without copying them.

Tools (the Claude Code `SessionStart` hook and the Codex actions) call these scripts directly with `python3`; there is no separate per-tool wrapper layer. `python3` must be available on `PATH`.

## Setup Behavior

`scripts/agents/setup-local-env.py` is intentionally lightweight:

- prints the current project directory;
- prints Java and Gradle wrapper versions;
- makes `./gradlew` executable on Unix;
- writes ignored `local.properties` when `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to an Android SDK-like directory, warning if common SDK markers are missing;
- runs `./gradlew --no-daemon help`.

It does not run full `check`, emulators, desktop/web app launches, screenshot capture, device checks, or UI recordings.

## Validation Modes

Use `python3 scripts/agents/validate.py` with one of these modes:

- `fast` runs Gradle `help` and the agent-instruction validator.
- `full` runs the full root Gradle `check`.
- `docs` runs the agent-instruction validator.
- `architecture` runs the Gradle `konsistCheck` task.

A lightweight CI job (`Agent instructions` in `.github/workflows/ci.yml`) runs `validate-agent-instructions.py` on changes to instruction files, so the `CLAUDE.md` shim and `AGENTS.md`/`CLAUDE.md` pairing invariants are enforced automatically. This replaces the former Konsist hygiene test, which duplicated the same checks.

For module-local code changes, prefer the smallest affected Gradle task from `AGENTS.md` over a broad aggregate script mode.

The shared scripts run under `python3`. `validate.py` additionally honors a `PYTHON` environment variable as an override for the interpreter it uses to run `scripts/agents/validate-agent-instructions.py`.

## Config Policy

- Codex Local Environment config is committed at `.codex/environments/environment.toml`.
- The committed Codex automatic setup currently uses the generated single `[setup].script` shape. Official Codex docs describe platform-specific setup scripts, but this repository has no confirmed TOML shape for platform-specific automatic setup, so Unix setup remains an explicit manual action until Codex App generates or documents that shape.
- Future app-generated `.codex` changes must be reviewed before committing.
- Claude Code project config is committed at `.claude/settings.json`. It holds safety-oriented `permissions.deny` rules (local env files, secrets, keystores, certificates, private keys, `local.properties`) plus the minimum worktree wiring needed for agents to work in this repository: `worktree.baseRef: "head"` and one `SessionStart` hook that recreates the `.claude/skills` link. Everything else stays out.
- Worktree safety and per-machine preferences stay in `.claude/settings.local.json` (which overrides the committed file): `worktree.bgIsolation` lives there, never committed, so the default isolation guard holds repo-wide. `.worktreeinclude` (git-ignored paths to seed into new worktrees) and the `scripts/agents/link-claude-skills.py` helper are committed, shared repo infrastructure. See `docs/agents/claude-code.md` for current values and rationale.
- The committed `SessionStart` hook is the single deliberately approved committed hook (it only maintains the skills link). Any further committed hook is a separate deliberate decision and must be documented here.
- Keep Claude-specific repository behavior in `docs/agents/claude-code.md` and the thin `CLAUDE.md` shim.
- Keep Codex-specific setup notes in `docs/agents/codex-local-environment.md`.
- Do not copy `.agents/skills` into `.claude/skills` or another tool-specific tree. A local, git-ignored link created by `scripts/agents/link-claude-skills.py` is allowed because it points at the canonical tree instead of duplicating it.

Local-only preferences may live in ignored files such as `CLAUDE.local.md` or `.claude/settings.local.json`.

## Aggregate Task Decision

The repository has root `check` and `konsistCheck` tasks, which `validate.py` maps to its `full` and `architecture` modes. Adding new aggregate Gradle tasks (for example `verify` / `verifyDocs` / `verifyArchitecture`) is future deliberate build-logic work, not part of this docs/scripts-only environment layer.

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
