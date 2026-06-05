---
name: commit-preparation
description: Use this skill when preparing Sensee commits, writing commit messages, checking whether a diff should be split, or reviewing staged/unstaged changes before commit; follow docs/engineering/commits.md and avoid git state-changing commands unless explicitly requested.
---

# Commit Preparation

Status: active.

Use `docs/engineering/commits.md` as the canonical convention.
Do not run `git add`, `git commit`, `git reset`, `git rebase`, `git commit --amend`, or `git push` unless the user explicitly asks.

## Operating mode

Review/support. Inspect and propose commit boundaries/messages; do not change git state unless explicitly asked.

## Non-goals

- Do not duplicate the full commit convention from `docs/engineering/commits.md`.
- Do not stage, commit, rewrite history, or push as part of preparation.

## Workflow

1. Inspect `git status --short` and the relevant diff.
2. Classify whether the diff is one logical change.
3. Flag breaking changes for the client app: persisted data without a migration, deep link/URL scheme, backend contract, or supported target/platform.
4. Identify unrelated formatting, generated output, local files, or secrets.
5. Check whether docs/ADR/LikeC4 updates are included when behavior or architecture changed.
6. Propose commit split and messages.
7. Report verification that was run or still should be run.

## Message rules

- Use `type(scope): imperative summary`; the header is the changelog line.
- Prefer scopes from real repo areas: `feature-practice`, `database`, `srs`, `ai`, `verification`, `gradle-plugins`, `c4`, `agents`, `ci`, `hooks`.
- Keep the header in English, lowercase description, no trailing period.
- Mark a breaking change with `type(scope)!:` and/or a `BREAKING CHANGE:` footer.
- Subject-only by default. Add a free-form body only for a non-obvious why; add footers (`Closes/Refs: EB-<N>`, `Refs: ADR-<NNN>`, `BREAKING CHANGE:`) only when they apply. Full rules in `docs/engineering/commits.md`.
- Do not invent motivation that is not visible from the task or diff.

## Output

- Diff summary
- Suggested commit split
- Commit message(s)
- Breaking-change marker and any EB/ADR footer
- Suspicious files
- Suggested verification
