---
name: pr-diff-review
description: Use this skill when reviewing the current Sensee diff before finalizing, committing, or opening a PR, or when the user asks for a diff review; operate in review-only mode and report correctness, architecture, tests, docs, LikeC4, Gradle, security, and unnecessary-change findings by severity.
---

# PR Diff Review

Status: active.

## Operating mode

Review only. Do not edit files unless the user explicitly asks.

## Non-goals

- Do not turn every small diff into a full architecture audit.
- Do not propose commit messages unless the user asks for commit preparation.

## Review level

- Default to a pragmatic senior-level review for ordinary changes: correctness, regressions, missing tests, unnecessary churn, local readability, and fit with nearby patterns.
- Escalate to principal-level systemic review only when the diff materially touches architecture, build/Gradle, module boundaries, public APIs, persistence, navigation, observability, LikeC4, CI, security, or cross-feature contracts.
- Do not turn every small localized diff into an architecture audit. If the scope does not justify systemic concerns, say that and keep the review focused.

## Workflow

1. Inspect `git diff` and `git status` with a safe-directory override if needed.
2. Determine whether the diff needs only the default review level or principal-level systemic review.
3. Read the closest `AGENTS.md` for touched files.
4. Check the diff against nearby tests, module boundaries, and docs.
5. Lead with findings, ordered by severity.
6. Include file and line references where possible.

## Always check

- Correctness and user-visible behavior.
- Missing or weak tests for behavior changes.
- KMP source-set placement and platform leakage.
- Feature/module dependency direction.
- Public API or navigation contract changes.
- Coroutine cancellation, broad exception handling, and Flow state/event misuse.
- SQLDelight schema ownership and migration impact.
- FSRS/SRS scheduling invariants.
- AI/TTS/verification seam boundaries and availability handling.
- Logging, analytics, crash reporting, secrets, and sensitive data.
- Gradle/CI impact.
- LikeC4/docs drift when architecture changes.
- Unrelated formatting, generated files, or local artifacts.

## Principal-level escalation

When the scope justifies escalation, also check:

- Whether the change creates or weakens an architectural boundary.
- Whether a local fix hides a cross-module contract change.
- Whether docs, ADRs, scenarios, or LikeC4 need to move with the implementation.
- Whether verification should widen from module-local checks to root, CI, or architecture checks.

## Output

- Blocking
- Important
- Nice to have
- Questions
- Suggested verification
