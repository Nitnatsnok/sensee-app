# Sensee

**Kotlin Multiplatform + Compose Multiplatform** learning app — a public **engineering
portfolio**, not a commercial product. It shows how I structure a modern KMP codebase,
share UI and logic across platforms, and how I make and document architectural decisions.

## Status

Demo slice, fixture-backed, no live backend yet.

- Several user-facing slices work end to end through real client boundaries (Ktor
  `HttpClient`, fixture-backed mock transport, SQLDelight persistence): the **Practice**
  flow (deck / home / card detail) on an FSRS-based SRS engine; **vocabulary capture**
  (term → AI-candidate senses → multi-select → durable confirm), with the AI-enrichment
  seam degrading offline to a deterministic fixture; the **Library** catalog with deck
  adoption; and **Profile** AI/TTS provider settings with key verification.
- Only **Home** is a navigable placeholder. Deeper surfaces (catalog curation, the full
  settings screen, lemma families, derived cards) are intentionally documented ahead of
  code as authoring-first (ADR-001), with implemented vs planned marked honestly.

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

iOS is not distributed: an installable build requires Apple Developer ID
signing, out of scope for this WIP. All artifacts are reviewer-facing WIP
snapshots, not product-tested.

`apps/androidApp/debug.keystore` is committed on purpose: it is the well-known
**public** Android debug key (`androiddebugkey` / `android`), not a secret. It
gives the APK a stable signature across CI builds so reviewers can update in
place. A secret-scanner flagging it is an expected false positive.

## Reviewing this repo? Start here

- **The real working slices:** [`shared/feature/practice`](shared/feature/practice) +
  [`shared/srs`](shared/srs) (FSRS engine with tests), and
  [`shared/feature/vocabulary-editor`](shared/feature/vocabulary-editor) (capture →
  AI candidates → confirm, with tests).
- **Decisions & trade-offs:** [`docs/adr/`](docs/adr/index.adoc) — especially ADR-004
  (FSRS as the single scheduling source of truth, with the app-wide SR-semantics cost
  explicitly accepted) and ADR-005 (AI-enrichment boundary contract). The sequenced,
  costed plan is [`docs/evolution-backlog.adoc`](docs/evolution-backlog.adoc).
- **Architecture:** [`docs/`](docs/) — arc42 + C4 (LikeC4 sources in
  [`docs/c4/`](docs/c4)); behavioral scenarios in [`docs/scenarios/`](docs/scenarios).
- **Build & verification:** [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Architecture & decisions

The product thesis is **authoring-first** (ADR-001): the user captures and disambiguates
lexical material; learning cards are *derived* artifacts, AI suggestions are *candidates*
until confirmed (ADR-001 / ADR-005), and grammar cues are structured annotations. Practice
is the downstream consumer of that model; capture is the upstream authoring slice.

Documentation is deliberately honest about implemented vs planned state (arc42 §11 risks,
[`docs/evolution-backlog.adoc`](docs/evolution-backlog.adoc)). Architecture diagrams are
LikeC4 sources, not exported PNGs.
Decisions are recorded as ADRs with alternatives and consequences marked *realized* vs
*expected*.

## What it demonstrates

- KMP targeting **Android, iOS, Desktop JVM, Web JS, Web Wasm**
- Shared app shell and screen composition with Compose Multiplatform — no Material, a
  custom design system
- Navigation and component boundaries via Decompose; config-driven and target-oriented
  (ADR-003)
- DI with Metro; local persistence with SQLDelight (feature-owned schema, aggregated
  database — ADR-002)
- A fixture-backed Ktor boundary through shared DI (a real client seam without a live
  backend)
- Provider-agnostic AI/TTS seams: offline fixtures, OpenAI-compatible AI enrichment,
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

The real plan is the sequenced, costed backlog in
[`docs/evolution-backlog.adoc`](docs/evolution-backlog.adoc) (a mutable backlog artifact,
not an ADR — that separation is itself the policy, stated there). The grammar-domain
extraction, library-owned catalog, and `vocabulary-editor/domain` are done; near-term is
an ad-hoc/query practice session (EB-3) and a card-transcription field (EB-5). A live
backend, AI-assisted learning semantics, and broader feature coverage follow.

This is a deliberate engineering sample that keeps evolving toward a fuller product — not
a finished commercial app.

## License

Source-available, **all rights reserved** — see [`LICENSE`](LICENSE). Published as an
engineering work sample: you may read and review the code, but copying, modification,
redistribution, or reuse (commercial or non-commercial) requires prior written
permission from the author.
