# Agent Instructions for `shared/srs`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for spaced-repetition libraries.

## Scope

Applies to:
- `shared/srs/...`

## Local context

`shared/srs` contains pure cross-platform spaced-repetition libraries: `core`, `engine`, `fsrs`, `fsrs-engine`, and `test-kit`. It is consumed by practice data, but it has no Compose, DI, or persistence responsibilities.

## Local rules

- Read ADR-004 before touching `FsrsScheduler` or scheduling semantics.
- `FsrsScheduler` is the single scheduling source of truth. Practice sessions project queues from engine state; they do not schedule independently.
- Do not reintroduce same-session re-show for FSRS-graduated `Review` cards.
- Keep `New` cards on the learning-step path: `New + Again/Hard/Good` stays in `Learning`; only `Good` past the last step or `Easy` graduates to `Review`.
- Keep math in `fsrs`/`fsrs-engine`; keep contracts in `core`. Feature and presentation code depend on `core` abstractions, not FSRS internals.

## Local verification

- Scheduling changes need focused `commonTest` coverage in the affected SRS module.
- Prefer:
  ```shell
  .\gradlew.bat :shared:srs:fsrs-engine:allTests
  ```
- If a change affects contracts used outside SRS, also run the narrowest consuming-module check.

## Do not

- Do not "fix" `New + Good` directly to `Review`; that breaks the ADR-004 learning-step model.
- Do not ship scheduling behavior changes with compile-only verification.
- Do not move persistence, DI, or UI behavior into `shared/srs`.

## Related skills

- `.agents/skills/fsrs-srs-engine-review`
- `.agents/skills/kmp-module-boundary-review`
- `.agents/skills/architecture-docs-sync`
