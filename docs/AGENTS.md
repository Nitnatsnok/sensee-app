# AGENTS.md - docs

## Scope

These rules apply to everything under `docs/`.

The documentation describes the Sensee KMP client: Android, iOS, Desktop JVM, JS, and Wasm built with Kotlin Multiplatform and Compose Multiplatform.

## Core rules

- Write documentation in Russian.
- Describe the actually implemented state of the repository first, then describe prepared or planned directions.
- Do not present a target architecture as if it already exists.
- Keep code terms, module names, class names, source set names, and Gradle paths in their original form and wrap them in backticks.
- Keep client-facing documentation concise. Prefer a small number of clear entry points over long navigation pages and internal process notes.

## Keeping docs and code in sync

- Before making architectural claims, verify `settings.gradle.kts`, the relevant module `build.gradle.kts` files, and the actual files under `src/`.
- If you change `docs/arc42`, check whether `docs/c4` also needs an update.
- If you change `docs/c4`, check whether the text in `docs/arc42` has become outdated.
- Diagrams in `docs/c4/*.c4` are the source files for architecture visuals.
- Do not keep generated exports or tool-specific helper files in client documentation unless they are actually referenced or explicitly needed.
- If a code change affects architecture, module boundaries, workflow, or observable behavior, update the relevant documentation as part of the same task.
- Do not leave documentation knowingly stale after the implementation is finished.
- For exploratory or unstable work, it is acceptable to stabilize the code first and update the docs after that, but the final task state should leave docs either accurate or explicitly marked as `WIP` / planned.
- If the user updates documentation first and then asks for implementation, treat the documentation as the target specification unless the user explicitly calls it a draft.
- If such documentation is ambiguous, incomplete, or conflicts with the current code, call out the mismatch before implementing the disputed part.

## Style and structure

- Prefer short, verifiable statements over broad generic wording.
- Explicitly mark states like "implemented", "prepared", and "planned" when that distinction matters.
- When describing modules and scenarios, rely on real entry points, dependencies, and current user flows.
- If you add a list of future documentation improvements, keep it practical: what exactly should be added, where, and why.
- Keep internal planning notes, backlogs, and tool usage instructions out of the main client documentation flow unless the user explicitly wants them documented there.
