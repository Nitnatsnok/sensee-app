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
- Adaptive layout reads project abstractions — `LocalAdaptiveInfo`/`AppAdaptiveInfo` (`shared/ui/adaptive`) in app/feature layers, `LocalSenseeAdaptiveLayoutMetrics` in the design system — not ad-hoc width math or a raw `WindowSizeClass`.
- Common UI avoids Android-only APIs.
- Accessibility labels, roles, focus, and touch targets are considered.
- Component APIs are slot-based and no broader than needed.
- Preview/demo code is not mixed with production behavior.
- Text, icons, and layout remain stable across supported targets.

## Gotchas

- `IS_WEB_ACCESSIBILITY_ENABLED = false` (`apps/webApp`) is a deliberate Compose Multiplatform workaround, not a defect — do not flag missing web accessibility semantics as a bug.
- Haptics go through `LocalSenseeHaptics` (`shared/core/compose`), gated by the `hapticFeedbackEnabled` setting; trigger feedback through it rather than calling platform haptics directly.
- The design system builds on `composeunstyled`; `shared/ui/design-system/AGENTS.md` is the canonical home for its token and primitive rules.

## Verification

- Module-scoped checks for the touched layer:
  ```shell
  .\gradlew.bat :shared:ui:design-system:check
  .\gradlew.bat :shared:app-shell:check
  .\gradlew.bat :shared:core:compose:check
  ```

## Output

- UI/component API summary
- Design-system consistency issues
- Adaptive/accessibility risks
- Minimal suggested fix
- Verification commands or manual checks

## Related skills

Deep Compose methodology — use these when available in the running agent: `compose-state-authoring` for local state declaration, `compose-state-hoisting`/`compose-state-holder-ui-split` for state ownership, `compose-slot-api-pattern` for component APIs, `compose-stability-diagnostics`/`compose-recomposition-performance` for skippability and recomposition, `compose-side-effects` for effects, `compose-focus-navigation` for focus and accessibility.
