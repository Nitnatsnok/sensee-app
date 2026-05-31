# Commit Convention

This file is the canonical commit convention for `Sensee`.
Agents use it through `.agents/skills/commit-preparation`; root `AGENTS.md` links here instead of duplicating the rules.

## Format

Use a short Conventional Commits-style header:

```text
type(scope): imperative summary
```

Examples:

```text
feat(vocabulary-editor): persist confirmed senses
fix(srs): keep review cards out of same-session recycling
docs(c4): clarify current learning slice
build(gradle-plugins): wire compose stability reports
ci(actions): keep hook smoke test on tooling changes
```

## Types

- `feat` - new user-facing or domain behavior.
- `fix` - a defect or regression fix.
- `refactor` - structure changes without intended behavior changes.
- `test` - tests, test fixtures, or test infrastructure.
- `docs` - documentation, ADRs, scenarios, LikeC4, and agent-facing instructions.
- `build` - Gradle, version catalog, convention plugins, wrapper, or toolchain wiring.
- `ci` - GitHub Actions, Dependabot, hooks, or release workflow.
- `perf` - performance changes without external behavior changes.
- `style` - formatting-only changes.
- `chore` - supporting changes that do not fit the types above.

## Scope

The scope should help a reviewer understand the boundary of the change. Prefer real repository areas:

- `app-shell`, `android-app`, `desktop-app`, `web-app`, `ios-framework`
- `feature-practice`, `feature-library`, `feature-profile`, `feature-vocabulary-editor`
- `srs`, `database`, `ai`, `tts`, `verification`, `grammar`, `lexicon`, `settings`
- `ui-design-system`, `ui-learning-deck`, `core-network`, `core-decompose`, `core-observability`
- `gradle-plugins`, `quality`, `docs`, `c4`, `agents`, `ci`, `hooks`

If one scope is misleading, split the diff into multiple commits or use a broader scope only for a real cross-cutting change.

## Header

- Write the header in English.
- Use imperative mood: `add`, `fix`, `keep`, `wire`, `document`.
- Do not end the header with a period.
- Avoid vague summaries like `improve code quality`, `update stuff`, or `misc fixes`.
- Do not claim more than the diff and task context support.

## Body

Add a body when the header is not enough:

- explain why the change exists;
- list important behavior consequences;
- name affected architecture boundaries, ADRs, or LikeC4 views;
- document migrations, constraints, manual checks, or known risk.

Recommended short template:

```text
Why:
- ...

Verification:
- ...
```

Do not write a body only to repeat the header.

## Commit Splitting

One commit should represent one logical reason for change. Usually split:

- production code and independent mechanical cleanup;
- behavior and unrelated refactoring;
- Gradle/convention plugin changes and feature code;
- agent infrastructure docs and user-facing documentation;
- generated artifacts and source files, if generated artifacts must be committed at all.

Do not split the test and implementation for one regression when they form one reviewable fix.

## Before Committing

- Check `git diff` and `git status` for accidental files.
- Ensure generated output from `build/`, `.gradle/`, `.kotlin/`, `kotlin-js-store/`, local properties, and secrets are not included.
- Check whether docs, ADRs, and LikeC4 need updates for architecture or user-visible behavior changes.
- Record verification that actually ran. Do not claim a command passed unless it was executed.

Agents must not run `git add`, `git commit`, `git reset`, `git rebase`, `git commit --amend`, or `git push` unless the user explicitly asks.
