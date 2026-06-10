---
name: gradle-convention-plugin-change
description: Use this skill when changing Sensee Gradle build logic, convention plugins, version catalog entries, KMP target setup, toolchains, detekt/ktlint/konsist/lint wiring, Compose stability reporting, LikeC4 validation wiring, or CI verification commands.
---

# Gradle Convention Plugin Change

Status: active.

## Operating mode

Implementation support. Keep changes narrow and verify build-logic behavior before widening checks.

## Non-goals

- Do not upgrade Kotlin, AGP, Compose, ktlint, detekt, or toolchains casually.
- Do not weaken quality checks to make one module pass.

## Workflow

1. Read `gradle-plugins/AGENTS.md`, root `build.gradle.kts`, `settings.gradle.kts`, and the affected plugin files.
2. Identify whether the change affects only the included build or also consuming modules.
3. Prefer lazy APIs and existing local DSL helpers.
4. Update contributor docs when commands or workflow change.
5. Verify with the narrowest affected Gradle task.

## Check

- `Provider`, `Property`, task registration, and `configureEach` are preferred.
- `afterEvaluate` is avoided unless justified.
- Plugin application is explicit and predictable.
- Dependency and plugin versions stay in `gradle/libs.versions.toml`.
- Task dependencies are explicit.
- Behavior remains compatible with existing Android/KMP/JVM modules.
- Generated output stays under `build/`.
- LikeC4 or CI command wiring uses existing package manager/Gradle conventions.

## Gotchas

- Convention plugin ids (`app.sensee.gradle.*`) and their responsibilities live in `gradle-plugins/AGENTS.md` and root `AGENTS.md`; read those instead of re-deriving the inventory.
- Wasm incremental compilation is intentionally disabled (`kotlin.incremental.wasm=false` in `gradle.properties`) as a KT-85270 workaround; do not re-enable it to "clean up" (tracked in `docs/backlog/`, EB-19).
- `composeStabilityReport` emits compiler reports only when the task is invoked by name (it inspects `startParameter.taskNames`); a normal compile deliberately skips them, so stability reports are not a build side-effect.

## Verification

- Build logic:
  ```shell
  .\gradlew.bat -p gradle-plugins :plugin:check
  ```
- If quality wiring changes:
  ```shell
  .\gradlew.bat ktlintCheck detekt konsistCheck
  ```
- If Android lint wiring changes, include:
  ```shell
  .\gradlew.bat lint
  ```
- If Compose stability reporting changes, include:
  ```shell
  .\gradlew.bat composeStabilityReport
  ```

## Output

- Build logic impact
- Risky Gradle patterns
- Compatibility concerns
- LikeC4/docs build impact
- Minimal recommended change
- Verification commands
