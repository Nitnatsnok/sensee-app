---
name: kmp-source-set-review
description: Use this skill when Sensee work touches commonMain, androidMain, iosMain, jvmMain, jsMain, wasmJsMain, webMain, nativeMain, expect/actual APIs, platform dependencies, or Compose Multiplatform target behavior.
---

# KMP Source Set Review

Status: active.

## Operating mode

Review/support. Prefer moving behavior to the narrowest valid source set before adding abstractions.

## Non-goals

- Do not add `expect/actual` when existing common APIs or source-set placement are enough.
- Do not broaden platform dependencies beyond the source set that needs them.

## Workflow

1. Read the affected module `build.gradle.kts` and source-set directories.
2. Identify the narrowest source set that can own the behavior.
3. Check imports for platform leakage.
4. Recommend the smallest move, dependency placement, or `expect/actual` boundary.

## Check

- `commonMain` has no JVM/Android/browser/Apple-only APIs.
- `expect/actual` exists only for real platform boundaries.
- Platform code lives in matching source sets: `androidMain`, `iosMain`, `jvmMain`, `jsMain`, `wasmJsMain`, `webMain`, or `nativeMain`.
- Dependencies are attached to the narrowest valid source set.
- Compose common UI avoids Android-only APIs and resources.
- SQLDelight, Ktor, serialization, and Compose dependencies match target capabilities.
- Platform context does not leak into domain models.

## Output

- Source-set issues
- Platform leakage
- Dependency placement suggestions
- Expected verification tasks
