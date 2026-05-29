# AGENTS.md

## Project overview

Sensee is a Kotlin Multiplatform client built with Compose Multiplatform.
The repository targets Android, iOS, Desktop JVM, JS, and Wasm.

Primary goals when changing code:

- Keep shared behavior in `commonMain` whenever possible.
- Add platform-specific code only in the matching source set (`androidMain`, `iosMain`, `jvmMain`, `jsMain`, `wasmJsMain`).
- Prefer small, module-local changes over cross-cutting refactors unless the task explicitly requires broader architectural work.

## Repository map

- `apps/androidApp` - Android application entry point.
- `apps/desktopApp` - Desktop JVM application entry point (Kotlin/JVM + Compose Desktop).
- `apps/webApp` - Web application entry point (Compose Multiplatform JS and Wasm targets).
- `apps/iosFramework` - iOS framework export module (`SenseeKit`) consumed by the Xcode host.
- `apps/iosApp` - Xcode host application for iOS.
- `shared/app-shell` - main shared application shell, root UI composition, platform environments, and app wiring.
- `shared/core` - reusable infrastructure modules such as DI, networking, observability, and Decompose integration.
- `shared/database` - SQLDelight-based persistence layer.
- `shared/feature` - feature modules.
- `shared/srs` - spaced repetition domain and engine modules.
- `shared/ai` - AI enrichment seam (`core` contract, curated/LLM implementations, `integration` composition).
- `shared/tts` - text-to-speech seam (`core` contract, provider/playback implementations, `integration` composition).
- `shared/verification` - lexical verification seam (`core` contract, dictionary/network providers, `integration` composition).
- `shared/grammar` / `shared/settings` - neutral grammar taxonomy and user-settings domain/data.
- `shared/ui` - reusable UI libraries such as design system, adaptive UI, and learning deck.
- `gradle-plugins` - local convention plugins and shared Gradle build logic.
- `docs` - project documentation. This subtree has its own `docs/AGENTS.md`; the closer file takes precedence there.

## Environment and toolchain

- Use the Gradle wrapper from the repo root: `.\gradlew.bat` on Windows, `./gradlew` on macOS/Linux.
- Dependency and plugin versions are defined in `gradle/libs.versions.toml`; do not duplicate version numbers in this file.
- JVM toolchain is Java `21` in `apps/desktopApp`; keep new JVM-targeted code compatible with that toolchain.

## Module & source-set conventions

Source sets:

- Keep business logic, state, contracts, and data transformations in `commonMain` by default; use platform source sets (`androidMain`, `iosMain`, `jvmMain`, `jsMain`, `wasmJsMain`) only for APIs that genuinely require platform access (filesystem, browser, Android components, Apple frameworks).
- For a new platform-specific abstraction, define the contract in `commonMain` first, then provide platform implementations.

Module boundaries:

- UI composition belongs in `shared/app-shell` or `shared/ui/*`.
- Navigation and component infrastructure belongs in `shared/core/decompose`; persistence in `shared/database`.
- Shared network clients, engines, headers, logging, and mock transport wiring belong in `shared/core/network` and `shared/core/mock-backend`; features inject data sources or repositories instead of constructing `HttpClient`.
- External-integration seams (`shared/ai`, `shared/tts`, `shared/verification`) follow one convention: `<area>/core` is the provider-agnostic boundary contract (no vendor SDK, no feature domain); sibling modules are implementations (curated/offline modules, playback/cache modules, backend-served curated reference data, or one adapter per external API); `<area>/integration` is the single composition module the app depends on — it `api`-aggregates `core` + impls and holds implementation selection (routing-by-config), settings adapters, and Metro `@Provides`/`@ContributesTo`. The composition root (`shared/app-shell`) depends only on `<area>/integration`, never on `core`/impls directly.
- Feature modules live under `shared/feature/<feature-name>/`; every path segment uses lowercase kebab-case.

Feature module layout — add submodules only when justified, never by template:

- `presentation/api` — feature-facing component/UI contracts. Keep feature entry configs here when the only external caller is the host that creates the component.
- `presentation/impl` — UI implementations.
- `presentation/navigation-api` — extract only when other modules need navigation entry contracts (`ScreenConfig`, route ids, typed open contracts) without depending on component-hosting API.
- `domain` / `data` — optional; add only when the feature has responsibilities worth isolating.
- `database-schema` — feature-owned SQLDelight schema while runtime database access stays in `shared/database`.
- Do not create empty `domain`/`data`/`database-schema` modules for symmetry.
- Decompose configs that can enter a persisted root or section stack must be serializable and registered in the common serializer assembly under `shared/core/decompose`.

Dependency direction:

- `presentation/impl` may depend on `presentation/api`, `presentation/navigation-api`, feature `domain`.
- `presentation/api` and `presentation/navigation-api` stay lightweight and never depend on `presentation/impl`.
- `data` may depend on feature `domain` and shared infrastructure; `domain` never depends on `presentation/*`; `database-schema` is schema-only and never depends on presentation.

## Build and verification commands

Start narrow, then widen. For a focused change prefer task-level commands (`compileKotlinJvm`, `compileKotlinJs`, `detekt`, `ktlintCheck`, the affected module's tests); for a stabilized module-local change use module-scoped `check`; reserve root `check` for final polishing, publish/commit boundaries, or changes crossing many modules. Root `check` is expensive in this KMP repo and produces large logs — do not run it reflexively after every edit.

Commands below use `.\gradlew.bat` (Windows); on macOS/Linux use `./gradlew` instead.

- Full verification: `.\gradlew.bat check`. This runs module checks, tests, Android Lint, ktlint, detekt, and the root `konsistCheck` aggregate.
- Aggregated tests: `.\gradlew.bat allTests`
- Android Lint: `.\gradlew.bat lint`
- Detekt: `.\gradlew.bat detekt`
- Kotlin style: `.\gradlew.bat ktlintCheck`
- Kotlin style auto-format: `.\gradlew.bat ktlintFormat`
- Architecture checks: `.\gradlew.bat konsistCheck`
- JVM tests: `.\gradlew.bat jvmTest`
- JS browser tests: `.\gradlew.bat jsBrowserTest`
- Wasm browser tests: `.\gradlew.bat wasmJsBrowserTest`
- Android app debug build: `.\gradlew.bat :androidApp:assembleDebug`
- Desktop run: `.\gradlew.bat :desktopApp:run`
- JS dev server: `.\gradlew.bat :webApp:jsBrowserDevelopmentRun`
- Wasm dev server: `.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun`

For targeted work, prefer module-scoped tasks such as:

- `.\gradlew.bat :shared:app-shell:check`
- `.\gradlew.bat :shared:database:check`
- `.\gradlew.bat :shared:core:network:check`
- `.\gradlew.bat :shared:core:mock-backend:check`
- `.\gradlew.bat :shared:srs:fsrs-engine:allTests`
- `.\gradlew.bat -p gradle-plugins :plugin:check` after changing local convention plugins or shared Gradle build logic (`gradle-plugins` is an included build, not a subproject of the root).

Quality checks are wired through local convention plugins:

- `app.sensee.gradle.quality` applies ktlint, detekt, and Android Lint defaults when an Android plugin is present.
- `app.sensee.gradle.quality.compose` adds Compose detekt rules for Compose modules.
- `app.sensee.gradle.kmp-library` applies the shared KMP library defaults and `quality`.
- `app.sensee.gradle.compose-multiplatform` applies Compose Multiplatform, the Compose compiler plugin, and Compose quality checks.

Static-check configuration lives in `config/detekt/detekt.yml`, `config/lint/lint.xml`, and `gradle-plugins/`. Keep those files in sync with the actual Gradle tasks.

## Tests and verifiability

- A behavior change (shared logic, feature logic, bug fix) ships its test in the
  same change, in the affected module's `commonTest` or matching platform test
  source set. The test must fail without the change — it has to catch the
  regression, not just execute the code.
- Name a test after the behavior it pins, readable as a sentence:
  ``fun `new card with again enters learning`()``. Copy
  `shared/srs/fsrs/FsrsSchedulerTest` as the reference; backtick names with
  spaces compile on every target here, so do not invent a per-module scheme.
- Keep tests minimal: one behavior per test, hand-written fakes over mock
  frameworks, no setup the test does not use.
- Work one behavior at a time — a test, then its implementation, then the next —
  not all tests first and then all the code.
- When designing or reshaping a user-facing feature, write or extend its Gherkin
  scenario in `docs/scenarios/` first (conventions in `docs/scenarios/README.md`).
  Refactors, infrastructure, and Gradle changes do not need a scenario.

## Implementation hygiene

- Keep code and configuration minimal, intentional, and scoped to the place where the behavior belongs.
- Before adding a new condition, option, rule, helper, or convention, check whether existing code, defaults, or project conventions already cover the case.
- Avoid overlapping mechanisms that express the same behavior in multiple ways. If overlap is required for a tool-specific or compatibility reason, keep the narrowest working form and make the reason clear in nearby code or configuration.
- Do not hide broad behavior inside narrow abstractions. Root-level or cross-module behavior should stay visible at the appropriate shared level; module-local code and conventions should avoid unrelated side effects.
- When simplifying code or configuration, verify the actual behavior with the smallest relevant test or Gradle task instead of assuming equivalent-looking rules behave the same.

## Editing rules

- Do not edit generated or build output manually:
  - `build/`
  - `.gradle/`
  - `.kotlin/`
  - `kotlin-js-store/`
  - generated files under module `build/generated/`
- Do not hand-edit `.idea/` unless the task is explicitly about IDE configuration.
- Respect nested `AGENTS.md` files anywhere in the repository. The closest `AGENTS.md` to the changed file takes precedence over the root file.
- Every `AGENTS.md` must have a sibling `CLAUDE.md` containing the single line `@AGENTS.md`. Claude Code does not auto-discover `AGENTS.md`; the shim makes the same rules apply when working under that directory in Claude Code. When you add a new `AGENTS.md`, create the shim in the same commit.
- The worktree may already contain user changes. Do not revert unrelated modifications.

## Code style

- Match the existing Kotlin style in the touched file instead of reformatting unrelated code.
- Keep package names, type names, and identifiers in English.
- Prefer explicit, descriptive names over abbreviations unless the code already uses a local abbreviation consistently.
- Keep Compose functions small and composable; lift state only when ownership actually changes.
- Avoid introducing new dependencies or new modules unless the task justifies the cost.

## Documentation

- `docs/` has its own `docs/AGENTS.md` (Russian, doc-first rules) — the closer file governs work there.
- When a code change alters architecture, module boundaries, workflow, or user-visible behavior, update the relevant docs in the same task; do not leave docs knowingly stale. Docs may follow code for unstable work, but the final state must be accurate or explicitly marked `WIP`/planned.
