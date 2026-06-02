# Contributing / build & verification

Operational detail for working in this repository. The [`README.md`](README.md) stays
high-level on purpose; the mechanics live here. Agent/contributor conventions are in
[`AGENTS.md`](AGENTS.md) (and nested `AGENTS.md` files, which take precedence in their
subtree).

## Prerequisites

A standard Kotlin Multiplatform setup is assumed and not repeated here: JDK 21, the
Android SDK (`ANDROID_HOME`) for Android, Xcode on macOS for iOS, and Git. The Gradle
wrapper is bundled.

Beyond that, this repository needs a few extra tools:

- **`python3`** (on `PATH`) — runs the agent environment scripts (`scripts/agents/*.py`),
  the Claude Code `SessionStart` hook, and the agent-instruction validator.
- **lefthook** — runs the tracked git hooks; bootstrapped by
  [`scripts/setup-git-hooks.sh`](scripts/setup-git-hooks.sh), which can install it for you.
- **gitleaks** — the secret scan run by the `pre-push` hook.

Per feature, only if you use that tool:

- **Node.js / `npx`** — LikeC4 (`npx likec4 validate/start docs/c4`) and the LikeC4 MCP
  server (`.mcp.json`).
- **`uv`** — runs the graphify knowledge graph (`uv tool install graphifyy`; tested with
  graphify `0.8.x`). Drives the pre-commit graph rebuild and the graphify MCP server; both
  skip gracefully when graphify is absent.

## Running per platform

Use the Gradle wrapper from the repository root.

| Target | macOS/Linux | Windows |
|---|---|---|
| Android | `./gradlew :androidApp:assembleDebug` | `.\gradlew.bat :androidApp:assembleDebug` |
| Desktop JVM | `./gradlew :desktopApp:run` | `.\gradlew.bat :desktopApp:run` |
| Web JS | `./gradlew :webApp:jsBrowserDevelopmentRun` | `.\gradlew.bat :webApp:jsBrowserDevelopmentRun` |
| Web Wasm | `./gradlew :webApp:wasmJsBrowserDevelopmentRun` | `.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun` |

iOS requires macOS with Xcode: open [`apps/iosApp`](apps/iosApp) and run the host app.

## Verification

`check` is the default full verification command: compilation and tests together with
ktlint, detekt, Android Lint, and Konsist architecture checks.

For day-to-day work, prefer targeted module checks first (e.g.
`./gradlew :shared:app-shell:check`, or narrower `compileKotlinJvm` / `detekt` /
`ktlintCheck` / module tests). Full `check` is best for final polishing, before
publishing, or after broad cross-module changes — it is expensive in this KMP repo.

Useful tasks (prefix with `.\gradlew.bat` on Windows):

```
./gradlew check
./gradlew allTests
./gradlew detekt
./gradlew ktlintCheck
./gradlew lint
./gradlew konsistCheck
./gradlew composeStabilityReport
./gradlew jvmTest
./gradlew jsBrowserTest
./gradlew wasmJsBrowserTest
```

After changing local convention plugins or shared Gradle build logic, also run
`./gradlew -p gradle-plugins :plugin:check`.

Static-check configuration:

- [`config/detekt/detekt.yml`](config/detekt/detekt.yml)
- [`config/lint/lint.xml`](config/lint/lint.xml)
- shared Compose stability baseline in [`config/compose/stability.conf`](config/compose/stability.conf)
- local Gradle convention plugins in [`gradle-plugins`](gradle-plugins)

## Compose stability reporting

Compose modules that apply
[`app.sensee.gradle.compose-multiplatform`](gradle-plugins/plugin/src/main/kotlin/app/sensee/gradle/ComposeMultiplatformPlugin.kt)
consume the shared baseline together with a module-local `compose-stability.conf` file and
the same file from direct project dependencies. App-level aggregation alone is not enough
for shared Compose modules such as [`shared/app-shell`](shared/app-shell): Compose
stability is inferred per compiled module, not only at the final app boundary. Keep
exported Decompose component contracts in the owning module's `compose-stability.conf` so
downstream Compose modules inherit the declarations without duplicating them.

Each Compose module gets a `composeStabilityReport` task; a module run such as
`./gradlew :shared:app-shell:composeStabilityReport` writes compiler outputs to
`build/reports/compose_compiler`. The root `collectComposeStabilityReports` aggregates
every Compose module's report into the root `build/reports/compose_compiler` tree grouped
by module path; the root `composeStabilityReport` runs module reports then aggregates.
Both report tasks refresh their compilation/aggregation even when otherwise up to date, so
`--rerun-tasks` is not required. The task is intentionally limited to Android, JVM, and
common-metadata compilations to avoid unrelated web/native toolchain setup.

## Git hooks

Tracked hook scripts live under [`.githooks`](.githooks) and are wired through
[`lefthook.yml`](lefthook.yml):

- `pre-commit` blocks generated output, local/secret-bearing files, merge-conflict
  markers, trailing whitespace, missing final newlines, and unusually large staged files.
  The tracked graphify artifacts (`graphify-out/graph.json` and
  `graphify-out/GRAPH_REPORT.md`) are exempt because they are committed generated
  output; the same hook incrementally rebuilds them from staged changed paths and
  stages them via `.githooks/graphify-rebuild`. Partial staging of a changed path
  uses a temporary staged-index checkout so unstaged edits do not enter the graph.
- `pre-push` runs `ktlintCheck`, `detekt`, `konsistCheck`; when Gradle build logic
  changes it also runs `./gradlew -p gradle-plugins :plugin:check`.

Scripts are POSIX shell run through Lefthook, so the same tracked hooks work on
macOS/Linux and on Windows via Git for Windows / Git Bash.

Enable once per clone:

```
sh ./scripts/setup-git-hooks.sh        # macOS/Linux
bash ./scripts/setup-git-hooks.sh      # Windows
```

If `lefthook` is missing, the bootstrap can offer a best-effort install through a detected
package manager, unset a conflicting local `core.hooksPath`, and run `lefthook install`.
It is interactive by default; for automation set
`SENSEE_GIT_HOOKS_INSTALL_MISSING_DEPS=never` (fail fast) or `=always` (auto-confirm). For
debugging, `pre-push` supports `SENSEE_GIT_HOOK_SKIP_GRADLE=1` (print selected tasks) and a
`--files ...` dry-run mode.

## GitHub CI

A first GitHub Actions workflow lives in
[`.github/workflows/ci.yml`](.github/workflows/ci.yml). It runs on pull requests, pushes
to `develop`, and manual dispatch, detects which areas changed, and runs only related jobs:
hook-tooling smoke tests, Gradle wrapper integrity, `ktlintCheck`, `detekt`,
`konsistCheck`, `jvmTest`, and `./gradlew -p gradle-plugins :plugin:check`. A separate
macOS job builds the iOS app with `xcodebuild` for the simulator and smoke-launches it via
`xcrun simctl` so iOS regressions are caught without a local Mac. This keeps CI focused on
fast, broadly useful feedback without the full Android/browser stack on every change.
