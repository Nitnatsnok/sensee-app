# shared/srs

Cross-platform spaced-repetition libraries: `core` (algorithm/model/log contracts),
`engine`, `fsrs` (FSRS implementation), `fsrs-engine`, `test-kit`. Consumed by
`shared/feature/practice/data`. No Compose, no DI, no persistence here — keep it a pure
algorithm library with its own tests (`commonTest`, `FsrsSchedulerTest`).

## Scheduling invariants — read ADR-004 before touching `FsrsScheduler`

`FsrsScheduler` is the **single source of truth** for review scheduling. The practice
session never schedules: it only *projects* a presentation queue from the card the engine
already wrote (see `DeckPracticeLogic.submitReview`). Do not add a second in-session
scheduler or a session-only re-show path — re-showing an FSRS-graduated `Review` card in
the same session corrupts difficulty/stability (elapsed ≈ 0 days). This was decided and
costed in ADR-004.

`scheduleCard` routes `New` through the **same learning-step path as `Learning`**
(`New, Learning -> scheduleLearningCard`). This is deliberate FSRS-with-learning-steps /
Anki behavior:

- `New + Again/Hard/Good` stays in `Learning`; only `Good` past the last step or `Easy`
  graduates to `Review`.
- **Do not "fix" this back to `New + Good -> Review`.** That stock behavior skips the
  steps, schedules a brand-new card days out after one answer, and breaks in-session
  recycling. It looks like a bug; it is the intended, ADR-recorded decision.

This is an app-wide spaced-repetition semantics choice. Changing state routing in
`scheduleCard`, the learning-step transitions, or the New-card branch is an architectural
change: update ADR-004 and `FsrsSchedulerTest` in the same task, never silently.

## Adding/altering algorithm behavior

- Keep math in `fsrs`/`fsrs-engine`; keep contracts in `core`. Presentation/feature code
  must depend only on `core` interfaces (`SrsScheduler`, snapshots), never on `fsrs`
  internals.
- Any scheduling change needs a `commonTest` case asserting the transition, not just a
  compile check — scheduling regressions are silent in the UI. The transition test ships
  with the change (see `Tests and verifiability` in the root `AGENTS.md`);
  `FsrsSchedulerTest` is the reference to copy.
