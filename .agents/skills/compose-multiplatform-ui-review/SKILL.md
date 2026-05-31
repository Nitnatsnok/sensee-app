---
name: compose-multiplatform-ui-review
description: Use this skill when Sensee work touches Compose Multiplatform UI, shared/app-shell UI composition, design-system primitives, adaptive layout, learning deck UI, themes/tokens, component APIs, accessibility semantics, or Compose preview/demo code.
---

# Compose Multiplatform UI Review

Status: active.

## Operating mode

Review/support. Recommend UI/API fixes first; implement only when the current task asks for changes.

## Non-goals

- Do not redesign unrelated screens.
- Do not introduce Material or new UI dependencies to solve local component issues.

## Workflow

1. Read the closest `AGENTS.md`; for design-system changes, read `shared/ui/design-system/AGENTS.md`.
2. Identify whether the change belongs in a feature screen, app shell, `shared/ui/learning-deck`, `shared/ui/adaptive`, or `shared/ui/design-system`.
3. Check state ownership, API surface, theme token use, and target compatibility.
4. Recommend the smallest UI/API correction.

## Check

- Component state ownership is clear and hoisted only when ownership changes.
- Design-system components use `SenseeTheme` tokens instead of raw colors/dimensions.
- No Material dependencies or `MaterialTheme` are introduced into the design system.
- Adaptive behavior uses project abstractions such as `LocalSenseeLayoutSizeClass`.
- Common UI avoids Android-only APIs.
- Accessibility labels, roles, focus, and touch targets are considered.
- Component APIs are slot-based and no broader than needed.
- Preview/demo code is not mixed with production behavior.
- Text, icons, and layout remain stable across supported targets.

## Output

- UI/component API summary
- Design-system consistency issues
- Adaptive/accessibility risks
- Minimal suggested fix
- Verification commands or manual checks
