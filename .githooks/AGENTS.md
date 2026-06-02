# Agent Instructions for `.githooks`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for tracked Git hook scripts.

## Scope

Applies to:
- `.githooks/...`

## Local context

Tracked hook scripts are run through Lefthook:
- `pre-commit` blocks generated output, local files, secrets, merge markers, trailing whitespace, missing final newlines, and oversized staged files. Only `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md` are exempt: they are committed generated artifacts, and `graphify-rebuild` incrementally regenerates them from staged changed paths before staging them in the same `pre-commit` run. It falls back to a temporary staged-index checkout only when the same path has both staged and unstaged edits.
- `pre-push` routes changed paths to Gradle verification and build-logic checks, and runs `scripts/agents/validate-agent-instructions.py` when agent-instruction files change (`AGENTS.md`/`CLAUDE.md`, `.agents/`, `docs/agents/`, `scripts/agents/`), so the shim and pairing invariants are gated locally and not only in CI.

## Local rules

- Keep scripts POSIX `sh` compatible; they run on macOS/Linux and on Windows through Git for Windows / Git Bash.
- Keep scripts non-interactive. Interactive setup belongs in `scripts/setup-git-hooks.sh`, not in hooks that run during Git operations.
- Preserve clear, actionable error messages and non-zero exit codes for guard failures.
- Keep path classifiers aligned with CI path filters and `CONTRIBUTING.md`.
- Use environment flags intentionally. `SENSEE_GIT_HOOK_SKIP_GRADLE=1` is for diagnostics and CI smoke tests, not for normal bypass behavior.

## Local verification

- Exercise changed hook paths with explicit files where possible, for example:
  ```shell
  sh ./.githooks/pre-commit README.md
  SENSEE_GIT_HOOK_SKIP_GRADLE=1 sh ./.githooks/pre-push --files build.gradle.kts
  ```
- CI also smoke-tests the hook tooling in `.github/workflows/ci.yml`.

## Do not

- Do not make hooks silently skip suspicious files.
- Do not require network access in hooks.
- Do not add shell features that Git Bash cannot run.

## Related skills

- `.agents/skills/commit-preparation`
- `.agents/skills/pr-diff-review`
