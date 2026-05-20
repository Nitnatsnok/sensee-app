package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.SenseeThemeMode

/**
 * Shared wrapper for every `@Preview` composable in the app.
 *
 * Every design-system component reads tokens through the [SenseeTheme] composition
 * locals, so a bare preview body would crash (or render with no styling). [SenseePreview]
 * provides the theme and a themed background fill so a preview is a single line:
 *
 * ```
 * @Preview
 * @Composable
 * private fun SenseeBadgePreview() = SenseePreview {
 *     SenseeBadge(text = "Irregular")
 * }
 * ```
 *
 * Use [themeMode] to author a dark counterpart next to the light one.
 */
@Composable
public fun SenseePreview(
    modifier: Modifier = Modifier,
    themeMode: SenseeThemeMode = SenseeThemeMode.Light,
    content: @Composable BoxScope.() -> Unit,
) {
    SenseeTheme(themeMode = themeMode) {
        Box(
            modifier =
                modifier
                    .background(SenseeTheme.colors.background)
                    .padding(SenseeTheme.spacing.large),
            content = content,
        )
    }
}
