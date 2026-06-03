# Sensee

Source-available **Kotlin Multiplatform + Compose Multiplatform** learning app sample.
It is not a commercial product. The repository focuses on shared client architecture,
cross-platform UI, local persistence, and documented architectural decisions.

## Status

Demo slice backed by fixtures; no live backend yet.

- Implemented: **Practice** (deck, home, card detail) with an FSRS-based SRS engine;
  **vocabulary capture** (term → AI candidate senses → multi-select → confirm);
  **Library** catalog and deck adoption; **Profile** AI/TTS provider settings with key
  verification.
- Ktor `HttpClient`, fixture transport, and SQLDelight are already wired through the
  client boundaries. When provider keys are missing, AI enrichment uses deterministic
  fixtures.
- **Home** is still a navigable placeholder. Catalog curation, full settings, lemma
  families, and derived cards are tracked as planned work in docs and ADRs.

## Try it without building

CI publishes reviewer artifacts so you don't need the KMP toolchain:

- **Web — live demo:** deployed to this repository's GitHub Pages (see the
  *github-pages* environment / the repo's Pages link).
- **Android / Desktop:** attached to the per-version
  [GitHub Release](../../releases):
  - `sensee-<version>-review.apk` — Android, release-like review build,
    debug-signed, installs on a device/emulator;
  - `*.msi` — Windows; unsigned, so SmartScreen shows *Unknown publisher* but
    it installs;
  - `*.deb` — Linux: `sudo apt install ./<file>.deb`.

iOS is not distributed because installable builds require Apple Developer ID signing.
Published artifacts are review builds, not product releases.

`apps/androidApp/debug.keystore` is committed on purpose: it is the well-known
**public** Android debug key (`androiddebugkey` / `android`), not a secret. It
gives the APK a stable signature across CI builds so reviewers can update in
place. A secret-scanner flagging it is an expected false positive.

## Reviewing this repo? Start here

- **Implemented slices:** [`shared/feature/practice`](shared/feature/practice) +
  [`shared/srs`](shared/srs) (FSRS engine with tests), and
  [`shared/feature/vocabulary-editor`](shared/feature/vocabulary-editor) (capture →
  AI candidates → confirm, with tests).
- **Decisions & trade-offs:** [`docs/adr/`](docs/adr/index.adoc) — especially ADR-004
  (FSRS scheduling) and ADR-005 (AI-enrichment boundary). Open follow-up work is in
  [`docs/backlog/`](docs/backlog/index.adoc).
- **Architecture:** [`docs/`](docs/) — arc42 + C4 (LikeC4 sources in
  [`docs/c4/`](docs/c4)); behavioral scenarios in [`docs/scenarios/`](docs/scenarios).
- **Build & verification:** [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Architecture & decisions

The product model is **authoring-first** (ADR-001): the user confirms lexical material
first, then cards and practice are built from confirmed meanings. AI suggestions stay as
candidates until confirmation (ADR-005), and grammar cues are structured annotations.

Docs separate implemented and planned state. Architecture diagrams are maintained as
LikeC4 sources, not exported PNGs. ADRs record alternatives and consequences.

## What it demonstrates

- KMP targeting **Android, iOS, Desktop JVM, Web JS, Web Wasm**
- Shared app shell and screen composition with Compose Multiplatform — no Material, a
  custom design system
- Navigation and component boundaries via Decompose; config-driven and target-oriented
  (ADR-003)
- DI with Metro; local persistence with SQLDelight (feature-owned schema, aggregated
  database — ADR-002)
- A Ktor client boundary backed by fixture transport, without a live backend
- AI/TTS provider boundaries: offline fixtures, OpenAI-compatible AI enrichment,
  provider-specific TTS adapters, and settings-backed routing
- Neutral grammar taxonomy and sense-first vocabulary capture with durable lexical
  storage
- A reusable learning-card deck: flip, swipe, keyboard, undo, configurable behavior
- Per-feature module layering: presentation `api` / `impl` / `navigation-api`, optional
  `domain` / `data` / `database-schema`

## Tech stack

Kotlin Multiplatform · Compose Multiplatform · Decompose · Metro DI · Ktor · SQLDelight ·
Kotlinx Serialization · Gradle convention plugins (`gradle-plugins/`).

Versions are maintained in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Repository structure

- `apps/androidApp` — Android entry point
- `apps/desktopApp` — Desktop JVM entry point (Kotlin/JVM + Compose Desktop)
- `apps/webApp` — Web entry point (Compose Multiplatform JS/Wasm)
- `apps/iosFramework` — iOS framework export (`SenseeKit`) consumed by the Xcode host
- `apps/iosApp` — Xcode host app for iOS
- `shared/app-shell` — shared application shell and root composition
- `shared/core` — infrastructure (networking, observability, Decompose/Compose integration, mock transport)
- `shared/database` — SQLDelight persistence layer
- `shared/feature` — feature modules
- `shared/srs` — spaced-repetition domain and engine modules
- `shared/ai` — AI enrichment seam (`core`, fixture, LLM adapter, integration)
- `shared/tts` — text-to-speech seam (`core`, playback/cache, provider adapters, integration)
- `shared/grammar` — neutral grammar taxonomy and labels
- `shared/settings` — typed user settings domain/data/schema
- `shared/ui` — reusable UI modules, including the learning deck
- `docs` — architecture documentation (arc42, ADR, C4, scenarios)

## Running

Use the Gradle wrapper from the repository root. Commands below use `./gradlew`; on
**Windows** use `.\gradlew.bat` instead.

- Android: `./gradlew :androidApp:assembleReview`
- Desktop JVM: `./gradlew :desktopApp:run`
- Web JS: `./gradlew :webApp:jsBrowserDevelopmentRun`
- Web Wasm: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- iOS: open [`apps/iosApp`](apps/iosApp) in Xcode (macOS) and run the host app.

## Verification

`./gradlew check` runs compilation and tests with the configured static checks (ktlint,
detekt, Android Lint, Konsist architecture checks). During development, prefer targeted
module checks first; use full `check` for final polishing or before publishing.

Git hooks, GitHub CI, the Compose-stability reporting tooling, and the full task catalog
are documented in [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Roadmap

Deferred work is tracked item-by-item in [`docs/backlog/`](docs/backlog/index.adoc) —
open items (e.g. sampled practice sessions, card transcription) plus ideas under
consideration. Larger planned directions — live backend/sync, external telemetry, and
live AI/TTS integrations — are described in the arc42 docs.

## License

Source-available, **all rights reserved** — see [`LICENSE`](LICENSE). Published as an
engineering work sample: you may read and review the code, but copying, modification,
redistribution, or reuse (commercial or non-commercial) requires prior written
permission from the author.
