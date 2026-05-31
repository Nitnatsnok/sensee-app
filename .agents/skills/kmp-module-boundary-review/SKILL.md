---
name: kmp-module-boundary-review
description: Use this skill when Sensee work touches Gradle project dependencies, feature modules, shared core modules, public API modules, domain/data/presentation boundaries, navigation-api modules, database-schema modules, or LikeC4 module relationships.
---

# KMP Module Boundary Review

Status: active.

## Operating mode

Review/support. Identify boundary drift and recommend the smallest ownership or dependency correction.

## Non-goals

- Do not create new modules for symmetry.
- Do not hide production boundary violations in test-only dependencies.

## Workflow

1. Read `settings.gradle.kts`, affected `build.gradle.kts` files, and closest `AGENTS.md`.
2. Map each touched module to its role: app, core, feature presentation/api/impl/navigation-api, domain, data, database-schema, integration, or provider impl.
3. Check source imports and Gradle dependencies for boundary drift.
4. Recommend the smallest dependency or ownership fix.

## Check

- Domain code does not depend on UI, platform, database, network implementation, or feature presentation.
- Presentation implementation depends on API/navigation/domain, not another feature implementation.
- Cross-feature communication goes through explicit APIs/contracts.
- `presentation/api` and `presentation/navigation-api` stay lightweight.
- `database-schema` modules remain schema-only.
- External seams keep `core` provider-agnostic and compose implementations in `integration`.
- Shared/core modules do not become dumping grounds.
- Test modules do not hide production boundary violations.
- LikeC4 is updated for significant boundary changes.

## Output

- Boundary summary
- Violations
- Suspicious but acceptable coupling
- LikeC4/docs impact
- Recommended minimal fix
- Tests/checks to run
