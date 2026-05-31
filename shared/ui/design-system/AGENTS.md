# Agent Instructions for `shared/ui/design-system`

This file extends the root `AGENTS.md`.
Follow the root rules first; this file only adds local rules for design-system primitives.

## Scope

Applies to:
- `shared/ui/design-system/...`

## Local context

`shared/ui/design-system` is the single home for visual primitives. Feature and screen code should compose `Sensee*` components rather than hand-rolling styling.

## Local rules

- Use `composeunstyled`, not Material. Do not introduce `androidx.compose.material*` or `MaterialTheme`.
- Read colors, spacing, typography, shapes, layout, and disabled alpha through `SenseeTheme`/design-system defaults; avoid raw hex/dp/sp styling in features.
- Keep the design system independent from `shared/ui/adaptive`; it consumes `LocalSenseeLayoutSizeClass` provided by app composition.
- Use the right header primitive for the context: `SenseePaneHeader` for wide detail panes, `SenseeSheetHeader` for modal sheets, and `SenseeTopBar` for full-screen stack navigation.
- Components should be stateless/slot-based where practical and expose styling through theme tokens plus `*Defaults`/`*Colors`.
- `*Colors` holders should stay explicit `@Immutable data class` values of `Color`.
- Consumers that call `painterResource()` or `Res.drawable.*` must declare `implementation(libs.compose.components.resources)` themselves.

## Local verification

- For component/API changes, run the design-system module check.
- For resource access changes, also compile the consuming module that imports the generated resource accessor.
- For adaptive or accessibility behavior, run the narrowest UI/module check and perform manual review when automated coverage is unavailable.

## Do not

- Do not inline feature-specific styling that should become a design-system primitive.
- Do not wrap pane or sheet headers in `SenseeTopBar` just to reuse colors or padding.
- Do not add companion color factories for single-variant components; keep named variant factories for genuinely multi-variant components.

## Related skills

- `.agents/skills/compose-multiplatform-ui-review`
- `.agents/skills/kmp-source-set-review`
