---
name: agent-task-slicer
description: Use this skill when a Sensee task is large, ambiguous, cross-module, or likely to mix architecture, docs, tests, Gradle, LikeC4, or UI work; slice it into safe independent steps with non-goals, dependencies, and verification before implementation.
---

# Agent Task Slicer

Status: active.

## Operating mode

Planning support. Produce slices and verification strategy; do not implement unless the user explicitly asks for implementation in the same task.

## Non-goals

- Do not replace module-specific review skills.
- Do not create broad refactor plans when a single coherent change is enough.

## Workflow

1. Restate the concrete outcome and current uncertainty.
2. Inspect only the files needed to map ownership:
   - root and closest `AGENTS.md`;
   - `settings.gradle.kts`;
   - relevant module `build.gradle.kts`;
   - nearby tests and docs/ADR/LikeC4 if architecture is involved.
3. Build a repository reality map:
   - affected modules and source sets;
   - owner boundaries: `presentation/api`, `presentation/impl`, `navigation-api`, `domain`, `data`, `database-schema`;
   - docs/ADR/LikeC4 impact;
   - likely verification tasks.
4. Propose slices that each leave the repository coherent.
5. Mark non-goals explicitly.
6. Recommend the first slice and the narrowest verification for it.

## Sensee-specific slicing rules

- Keep shared behavior in `commonMain` unless a platform API makes that impossible.
- Keep feature implementation details out of other feature implementation modules.
- Keep AI/TTS/verification composition in the matching `integration` module; app shell should depend on integration, not impl modules directly.
- Treat FSRS/SRS scheduling changes as their own slice with tests.
- Treat SQLDelight schema ownership/migration changes as their own slice.
- Treat Gradle convention plugin changes as their own slice verified with `.\gradlew.bat -p gradle-plugins :plugin:check`.
- Treat LikeC4 and architecture docs sync as a required slice when architecture changes.

## Output

- Repository reality map
- Proposed slices
- Non-goals
- Verification strategy
- Suggested first prompt for the next agent session
