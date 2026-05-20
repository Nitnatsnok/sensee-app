package app.sensee.ui.designSystem.component.badge

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme

/**
 * Color variants for [SenseeBadge].
 *
 * Each variant returns a tonal-light chip (light bg + comfortable on-color text). Sensee's
 * `*Container` tokens for `accent` / `accentSubtle` / `warning` are intentionally saturated
 * for tonal buttons; for chips that ride inline next to text we pick the `*Fixed` siblings
 * (theme-stable light tones with dark on-color). `success` / `info` / `danger` already use
 * light `*Container` in the light theme and adapt in dark theme, so they use `*Container`
 * directly.
 */
public object SenseeBadgeDefaults {
    public val BorderWidth: Dp = 1.dp

    // 10.dp / 3.dp keep the badge visually compact — they don't snap to a single spacing token,
    // but stay close to the `small`/`extraSmall` rhythm.
    public val ContentPadding: PaddingValues =
        PaddingValues(
            horizontal = 10.dp,
            vertical = 3.dp,
        )

    @Composable
    public fun shape(): Shape = SenseeTheme.shapes.circle

    @Composable
    public fun neutralColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
    ): SenseeBadgeColors {
        val colors = SenseeTheme.colors
        // Outlined variant: the fill is intentionally close to the surrounding card surface
        // tones, so a `divider` border is what makes the chip readable regardless of which
        // surface tier it sits on. Other variants don't need it — their saturated fills
        // already provide contrast.
        return SenseeBadgeColors(
            container = container.takeOrElse { colors.surfaceContainerHigh },
            content = content.takeOrElse { colors.textSecondary },
            border = border.takeOrElse { colors.divider },
        )
    }

    @Composable
    public fun accentColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
    ): SenseeBadgeColors {
        val colors = SenseeTheme.colors
        return SenseeBadgeColors(
            container = container.takeOrElse { colors.accentFixed },
            content = content.takeOrElse { colors.textOnAccentFixed },
            border = border.takeOrElse { Color.Transparent },
        )
    }

    @Composable
    public fun accentSubtleColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
    ): SenseeBadgeColors {
        val colors = SenseeTheme.colors
        return SenseeBadgeColors(
            container = container.takeOrElse { colors.accentSubtleFixed },
            content = content.takeOrElse { colors.textOnAccentSubtleFixed },
            border = border.takeOrElse { Color.Transparent },
        )
    }

    @Composable
    public fun warningColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
    ): SenseeBadgeColors {
        val colors = SenseeTheme.colors
        return SenseeBadgeColors(
            container = container.takeOrElse { colors.warningFixed },
            content = content.takeOrElse { colors.textOnWarningFixed },
            border = border.takeOrElse { Color.Transparent },
        )
    }

    @Composable
    public fun infoColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
    ): SenseeBadgeColors {
        val colors = SenseeTheme.colors
        return SenseeBadgeColors(
            container = container.takeOrElse { colors.infoContainer },
            content = content.takeOrElse { colors.textOnInfoContainer },
            border = border.takeOrElse { Color.Transparent },
        )
    }

    @Composable
    public fun successColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
    ): SenseeBadgeColors {
        val colors = SenseeTheme.colors
        return SenseeBadgeColors(
            container = container.takeOrElse { colors.successContainer },
            content = content.takeOrElse { colors.textOnSuccessContainer },
            border = border.takeOrElse { Color.Transparent },
        )
    }

    @Composable
    public fun dangerColors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
    ): SenseeBadgeColors {
        val colors = SenseeTheme.colors
        return SenseeBadgeColors(
            container = container.takeOrElse { colors.dangerContainer },
            content = content.takeOrElse { colors.textOnDangerContainer },
            border = border.takeOrElse { Color.Transparent },
        )
    }
}
