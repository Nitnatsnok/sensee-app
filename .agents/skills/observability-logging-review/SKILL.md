---
name: observability-logging-review
description: Use this skill when Sensee work touches AppDiagnostics, logger factories, log sinks, crash reporters, analytics trackers, breadcrumbs, logging tags, error reporting, platform diagnostics implementations, or sensitive diagnostic data.
---

# Observability Logging Review

Status: active; external telemetry backends are still planned.

## Operating mode

Review/support. Treat diagnostics as privacy-sensitive and cross-platform.

## Non-goals

- Do not introduce external telemetry backends as incidental work.
- Do not log user vocabulary, provider payloads, credentials, or raw secrets.

## Workflow

1. Read affected `shared/core/observability` contracts and platform implementations.
2. Identify whether the change belongs in the common facade, app-shell wiring, feature logic, or platform adapter.
3. Check sensitive data and cross-platform behavior.
4. Recommend narrow diagnostic context rather than broad logging.

## Check

- Domain code does not depend on concrete diagnostic backends.
- Common APIs can work on Android, iOS, Desktop JVM, JS, and Wasm.
- Tags are scoped and contextual, not duplicated everywhere.
- Sensitive data is not logged: API keys, user secrets, raw provider payloads when unnecessary, or private vocabulary examples.
- Breadcrumbs complement logs/crash reports rather than replacing actionable errors.
- Crash reports include enough context without leaking content.
- Analytics event names are stable and meaningful.
- Feature logic receives diagnostics through existing component/DI boundaries.
- LikeC4/docs are updated if observability becomes a documented subsystem change.

## Output

- Diagnostic contract impact
- Sensitive data risks
- Tag/breadcrumb quality
- Platform implementation gaps
- LikeC4/docs impact
- Recommended minimal fix
