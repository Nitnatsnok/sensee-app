package app.sensee.ui.designSystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.composeunstyled.platformtheme.bright
import com.composeunstyled.platformtheme.buildPlatformTheme
import com.composeunstyled.platformtheme.dimmed
import com.composeunstyled.platformtheme.indications

/**
 * Composable theme provider. Wraps [content] in the Sensee platform theme built around
 * `composeunstyled`'s `buildPlatformTheme`. The actual value tables live in dedicated files
 * so each category is quick to scan and review:
 *
 * - Colour palettes: [SenseeColorPaletteLight], [SenseeColorPaletteDark]
 * - Spacing scale: [SenseeSpacingScale]
 * - Layout metrics: [SenseeLayoutScale]
 * - Shape scale: [SenseeShapeScale]
 * - Typography: [SenseeTypographyScale] (+ `senseeTextStyles` assembly with Mulish)
 *
 * Co-named with [object SenseeTheme][SenseeTheme] (the token accessor) — M3-style: callers
 * write `SenseeTheme(themeMode = ...) { content }` to provide and `SenseeTheme.colors.accent`
 * to read.
 */
@Composable
public fun SenseeTheme(
    themeMode: SenseeThemeMode = SenseeThemeMode.System,
    content: @Composable () -> Unit,
) {
    val theme = remember(themeMode) { buildSenseePlatformTheme(themeMode) }
    theme(content)
}

private fun buildSenseePlatformTheme(themeMode: SenseeThemeMode) =
    buildPlatformTheme {
        name = "SenseeTheme"

        val isDark =
            when (themeMode) {
                SenseeThemeMode.System -> isSystemInDarkTheme()
                SenseeThemeMode.Light -> false
                SenseeThemeMode.Dark -> true
            }
        val palette = if (isDark) SenseeColorPaletteDark else SenseeColorPaletteLight
        val textStyles = senseeTextStyles()

        properties[SenseeThemeProperties.colors] = palette
        properties[SenseeThemeProperties.spacing] = SenseeSpacingScale
        properties[SenseeThemeProperties.shapes] = SenseeShapeScale
        properties[SenseeThemeProperties.typography] = textStyles
        properties[SenseeThemeProperties.layout] = SenseeLayoutScale

        defaultContentColor = palette.getValue(SenseeColorTokens.textPrimary)
        defaultTextStyle = textStyles.getValue(SenseeTypographyTokens.bodyMedium)
        defaultIndication =
            if (isDark) {
                properties[indications][bright]
            } else {
                properties[indications][dimmed]
            }
    }
