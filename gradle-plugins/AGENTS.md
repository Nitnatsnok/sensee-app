# Agent Instructions for `gradle-plugins`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for local Gradle build logic.

## Scope

Applies to:
- `gradle-plugins/...`

## Local context

`gradle-plugins` is an included build that owns Sensee convention plugins:
- `app.sensee.gradle.kmp-library`
- `app.sensee.gradle.compose-multiplatform`
- `app.sensee.gradle.kmp-web-toolchain`
- `app.sensee.gradle.mock-fixtures`
- shared quality convention scripts for detekt, ktlint, Compose, and public API checks.

## Local rules

- Prefer lazy Gradle APIs: `Provider`, `Property`, task registration, and `configureEach`.
- Avoid `afterEvaluate` unless there is a concrete Gradle lifecycle reason and a nearby comment explains it.
- Keep convention plugin behavior explicit and compatible with existing KMP modules.
- Do not change Kotlin, AGP, Compose, ktlint, detekt, or toolchain versions casually; versions belong in `gradle/libs.versions.toml`.
- Preserve configuration-cache friendliness and avoid reading project files during configuration unless the existing plugin pattern already requires it.
- When wiring generated sources or reports, keep generated output under `build/`.

## Local verification

- After local convention plugin changes:
  ```shell
  .\gradlew.bat -p gradle-plugins :plugin:check
  ```
- If the change affects repository-wide quality wiring, also consider:
  ```shell
  .\gradlew.bat ktlintCheck detekt konsistCheck
  ```
- If the change affects Android lint or Compose stability reporting, include the matching task:
  ```shell
  .\gradlew.bat lint
  .\gradlew.bat composeStabilityReport
  ```

## Do not

- Do not duplicate dependency or plugin versions in plugin code.
- Do not weaken quality checks to make one module pass; fix the module or narrow the convention deliberately.
- Do not edit generated Gradle output or wrapper internals unless the task is explicitly about wrapper maintenance.

## Related skills

- `.agents/skills/gradle-convention-plugin-change`
- `.agents/skills/kmp-source-set-review`
- `.agents/skills/pr-diff-review`
