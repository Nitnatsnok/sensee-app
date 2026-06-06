---
name: fsrs-srs-engine-review
description: Use this skill when Sensee work touches FSRS scheduling, shared/srs modules, review logs, card state transitions, due dates, clocks, rating logic, practice sessions, weak/new/overdue modes, or ADR-004 scheduling invariants.
---

# FSRS/SRS Engine Review

Status: active.

## Operating mode

Review/support. Treat scheduling behavior as invariant-sensitive and test-driven.

## Non-goals

- Do not change FSRS semantics as incidental cleanup.
- Do not accept compile-only verification for scheduling changes.

## Workflow

1. Read `shared/srs/AGENTS.md` and ADR-004 before reviewing behavior.
2. Identify whether the change is math/engine, practice domain/data, or presentation projection.
3. Check deterministic clock/id handling and review-log invariants.
4. Require focused tests for scheduling behavior.

## Check

- `FsrsScheduler` remains the single scheduling source of truth.
- UI and session presentation do not make scheduling decisions.
- New cards follow learning-step semantics; do not shortcut `New + Good` directly to `Review`.
- Same-session re-show of FSRS-graduated `Review` cards is not reintroduced.
- Clock and id generation are injectable/deterministic in tests.
- Review logs preserve invariants and enough evidence to audit transitions.
- Edge cases are covered: repeated review, overdue cards, same-day review, weak/new modes, and timezone/day-boundary handling when the change touches day bucketing or local-day logic.
- LikeC4/docs are updated if scheduling ownership or major flow changes.

## Output

- Scheduling invariant impact
- Domain/presentation separation risks
- Missing edge cases
- LikeC4/docs impact
- Test gaps
- Recommended minimal fix
