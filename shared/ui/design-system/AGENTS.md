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
