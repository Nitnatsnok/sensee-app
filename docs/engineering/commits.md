# Commit Convention

This file is the canonical commit convention for `Sensee`.
Agents use it through `.agents/skills/commit-preparation`; root `AGENTS.md` links here instead of duplicating the rules.

## Format

This convention conforms to Conventional Commits 1.0.0. The header is the
changelog line, so keep it short and self-sufficient:

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
feat(lexicon)!: identify senses by service-namespaced sense_id
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

`feat`, `fix`, `perf`, and breaking changes surface in the changelog; the rest
(`refactor`, `test`, `docs`, `build`, `ci`, `style`, `chore`) are housekeeping.

## Breaking changes

Sensee is a client app, not a published library, so "breaking" means what breaks
for an already-installed user on update, or at an external boundary, not internal
module APIs (those are refactors: every caller lives in this repo and is fixed in
the same change).

Mark a commit breaking when it:

- changes locally persisted data without a migration (DB schema, settings, secure
  storage, serialized Decompose configs), so an update loses data or resets state
  (no migrations yet, so today this means a dev reset);
- changes or removes an external entry point (deep link / URL scheme);
- breaks compatibility with a backend contract the client relies on;
- drops a supported target/platform or raises a minimum (minSdk, iOS version).

Indicate it with `!` before the colon and/or a `BREAKING CHANGE:` footer stating
what breaks and what the user or tester must do (reinstall, re-enter keys). In 0.x
this bumps the minor tag, but still mark it; it is the release-note signal.

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
- Start the description in lowercase.
- Avoid vague summaries like `improve code quality`, `update stuff`, or `misc fixes`.
- Do not claim more than the diff and task context support.

## Body

Optional and free-form (Conventional Commits rules 6-7): one blank line after the
description, any number of paragraphs. The spec only asks for "additional
contextual information about the code changes". Use a body when the header is not
enough to expand what the change does (scope, behavior consequences), give the
motivation, or note migrations, constraints, and known risk. Add only what the
header and diff do not already make obvious; do not narrate the diff line by line
or pad by restating the header. Verification output and observations go to the PR
or chat.

## Footers

Optional trailers, one blank line below the body (Conventional Commits rules
8-10). Use the `Token: value` form, with tokens using `-` instead of spaces.
Trace work with `Closes: EB-<N>` (the item this commit implements) or
`Refs: EB-<N>` / `Refs: ADR-<NNN>`. `BREAKING CHANGE:` is the only spaced,
uppercase token.

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
