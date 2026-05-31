# Agent Instructions for `.github`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for GitHub metadata.

## Scope

Applies to:
- `.github/...`

## Local context

The repository has GitHub Actions for CI and reviewer releases plus Dependabot management for GitHub Actions.

## Local rules

- Keep workflow permissions least-privilege and job-scoped.
- Existing actions are SHA-pinned. Preserve that convention when updating actions.
- Keep path filters aligned with real ownership: app/shared/build logic changes should still trigger verification, and hook/tooling changes should still exercise hook checks.
- Do not weaken `ktlintCheck`, `detekt`, `konsistCheck`, JVM tests, web tests, Android lint/build, or build-logic checks without a specific reason.
- Keep CI comments accurate when toolchain versions or runner constraints change.
- If verification commands change in workflows, update `CONTRIBUTING.md`, root `AGENTS.md`, and related skills when needed.

## Local verification

- For CI YAML-only edits, inspect workflow syntax and changed path filters.
- For hook-tooling edits, use the hook smoke-test path from `.github/workflows/ci.yml` or run the relevant hook script with `--files` when practical.
- For Gradle task changes in CI, run the narrowest affected Gradle command locally when feasible.

## Do not

- Do not add secrets to workflow files.
- Do not broaden release permissions across the whole workflow when one job can own the permission.
- Do not make macOS/iOS CI run by default unless the cost trade-off is explicit.

## Related skills

- `.agents/skills/gradle-convention-plugin-change`
- `.agents/skills/pr-diff-review`
- `.agents/skills/commit-preparation`
