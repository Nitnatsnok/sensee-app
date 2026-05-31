# Agent Instructions for `scripts`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for repository helper scripts.

## Scope

Applies to:
- `scripts/...`

## Local context

- `scripts/setup-git-hooks.sh` bootstraps Lefthook and tracked hooks. It is intentionally more interactive than Git hooks themselves, but supports environment variables for automation.
- `scripts/agents/validate-agent-instructions.py` validates a bounded set of agent-facing instruction and config files. It is read-only, non-interactive, and uses only the Python standard library.
- `scripts/agents/setup-local-env.*`, `list-tasks.*`, and `validate.*` are shared Codex / Claude Code local environment entrypoints.
- `scripts/codex/` and `scripts/claude/` contain thin wrappers only; shared behavior belongs in `scripts/agents/`.

## Local rules

- Keep scripts POSIX `sh` compatible unless a script declares another runtime explicitly.
- Prefer non-interactive automation paths with documented environment variables.
- Make destructive behavior opt-in and obvious.
- Keep dependencies discoverable and fail with clear messages when required tools are missing.
- If a script changes verification workflow, update `CONTRIBUTING.md`, root `AGENTS.md`, and CI/hook docs as needed.

## Local verification

- For `setup-git-hooks.sh`, use the CI smoke-test pattern in `.github/workflows/ci.yml` as the reference.
- For new scripts, include a safe `--help` path when practical and test it.
- For shared agent environment scripts, prefer syntax checks plus `scripts/agents/validate.* fast` before broader Gradle validation.
- After agent-instruction validator changes:
  ```shell
  python3 scripts/agents/validate-agent-instructions.py --help
  python3 scripts/agents/validate-agent-instructions.py
  ```
  On Windows:
  ```shell
  py -3 scripts/agents/validate-agent-instructions.py --help
  py -3 scripts/agents/validate-agent-instructions.py
  ```

## Do not

- Do not install global tools without asking in interactive mode or honoring the existing non-interactive environment controls.
- Do not make scripts depend on generated or local-only files.
- Do not hide required environment variables in code only; document them near the script or in `CONTRIBUTING.md`.

## Related skills

- `.agents/skills/code-comments-and-docs-review`
- `.agents/skills/pr-diff-review`
