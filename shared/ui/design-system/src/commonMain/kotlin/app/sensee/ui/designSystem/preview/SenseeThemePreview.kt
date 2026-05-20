package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.SenseeThemeMode
import com.composeunstyled.Text

@Preview
@Composable
private fun SenseeThemeLightPreview() =
    SenseePreview(themeMode = SenseeThemeMode.Light) {
        SenseeThemeShowcase()
    }

@Preview
@Composable
private fun SenseeThemeDarkPreview() =
    SenseePreview(themeMode = SenseeThemeMode.Dark) {
        SenseeThemeShowcase()
    }

@Composable
private fun SenseeThemeShowcase() {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium)) {
        Text(text = "Headline", style = typography.headlineSmall)
        Text(text = "Title large", style = typography.titleLarge)
        Text(text = "Body medium — the quick brown fox", style = typography.bodyMedium)
        Text(text = "Label large", style = typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
            ColorSwatch(colors.accent)
            ColorSwatch(colors.success)
            ColorSwatch(colors.warning)
            ColorSwatch(colors.danger)
            ColorSwatch(colors.info)
            ColorSwatch(colors.surfaceContainerHigh)
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(SenseeTheme.shapes.small)
                .background(color),
    )
}
