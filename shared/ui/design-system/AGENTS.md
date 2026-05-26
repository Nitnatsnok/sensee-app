# shared/ui/design-system

The single home for visual primitives. Feature/screen code composes `Sensee*` components;
it does not hand-roll styled `Box`/`Row`/`Text`. If a screen needs raw layout with
hardcoded colors/spacing/typography, that is a signal a design-system primitive is
missing — add it here, don't inline it in the feature.

## Theming — no Material

- Theming uses `composeunstyled`, **not** Material. Do not introduce `androidx.compose.material*`
  or `MaterialTheme` here or in consumers.
- `SenseeTheme` is both the token accessor (`SenseeTheme.colors` / `spacing` / `typography`
  / `shapes` / `layout`) and the `@Composable` provider (`SenseeTheme(themeMode = ...) { }`)
  — same idiom as `MaterialTheme`. Read tokens through it; never hardcode hex/dp/sp.
- Disabled-state alpha comes from `SenseeStateAlphas`, not ad-hoc `.alpha(0.38f)`.

## Adaptive size class

- Adaptive layout class is read via `LocalSenseeLayoutSizeClass`, provided by
  `AppComposeEnvironment` from `AppAdaptiveInfo`. The design system **must not depend on
  `shared/ui/adaptive`** — it only consumes the `Local`. `SenseeScreenContent` reads it by
  default so screens don't thread size class manually.

## Pane header vs sheet header vs top bar

The same logical "panel header" appears in three contexts. Use the design-system primitive
matching that context instead of hand-rolling a styled header in feature code:

- **`SenseePaneHeader`** — title row for a wide-layout detail/extra pane (`AppChildPanels`).
  It is layout only: the caller owns the pane surface, shape, status-bar inset, outer padding,
  and any divider/background chrome.
- **`SenseeSheetHeader`** — title row inside `SenseeModalBottomSheet`. The sheet owns the
  surface and drag indicator; the header only owns its inner row and content padding.
- **`SenseeTopBar`** — full window-edge app bar with a leading navigation slot. Use it for
  full-screen pages and the compact replace-main path of an `AppChildPanels` detail, where
  the detail behaves like a pushed page.

`SenseePaneHeader` and `SenseeSheetHeader` share the same structure:
`[ title ][ actions ][ close ]`. Close belongs in the trailing actions area, never in a
leading navigation slot. Back arrows belong to `SenseeTopBar` only, because back navigation
has stack semantics that a side panel or sheet close action does not.

For pane roots, prefer `SenseeSurface` or another design-system container over raw
`Modifier.background(...)`. The header should not be wrapped in `SenseeTopBar` just to get
colors, padding, or a close icon.

## Resources peculiarity (Compose Multiplatform)

- Drawables in `composeResources/drawable` auto-generate `Res.drawable.*` accessors
  (`publicResClass = true`).
- Any module that calls `painterResource()` / references `Res.drawable.*` — even when it
  only *consumes* resources defined here — must declare
  `implementation(libs.compose.components.resources)` in its `build.gradle.kts`. The
  generated accessors are importable, but the runtime `painterResource` function is not
  re-exported. Missing this dependency fails at the consumer, not here.

## Adding a component

- Keep it stateless/slot-based where possible; lift state to the caller.
- Expose styling only through theme tokens and a `*Defaults`/`*Colors` object, mirroring
  existing primitives (`SenseeButton`, `SenseeBadge`, `SenseeLearningCard`, …), so callers
  never pass raw colors.
- Canonical colour-set shape: a `@Composable <Component>Defaults.colors(... = Color.Unspecified)`
  factory that resolves each unspecified argument against `SenseeTheme.colors`
  (`SenseeSurfaceDefaults.colors()` is the reference). Component metrics (sizes, paddings,
  border widths, shape) sit as `val`s on the same `*Defaults` object. The one deliberate
  departure is `SenseeButtonColors` keeping `default()`/`tonal()`/`outlined()` variant
  factories on its companion: a button has several *named colour variants*, not one default
  set, so the variant lives with the colour type. Do not introduce companion colour
  factories for single-variant components.
- A `*Colors` holder is a plain `@Immutable data class` of `Color` (annotate it explicitly;
  do not rely on strong-skipping inference — every `Sensee*Colors` is annotated).
