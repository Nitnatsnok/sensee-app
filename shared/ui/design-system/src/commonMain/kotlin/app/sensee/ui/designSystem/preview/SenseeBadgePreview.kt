package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.badge.SenseeBadgeDefaults
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.SenseeThemeMode

@Preview
@Composable
private fun SenseeBadgePreview() =
    SenseePreview {
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
                SenseeBadge(text = "Neutral")
                SenseeBadge(text = "Accent", colors = SenseeBadgeDefaults.accentColors())
                SenseeBadge(text = "Subtle", colors = SenseeBadgeDefaults.accentSubtleColors())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
                SenseeBadge(text = "Success", colors = SenseeBadgeDefaults.successColors())
                SenseeBadge(text = "Warning", colors = SenseeBadgeDefaults.warningColors())
                SenseeBadge(text = "Danger", colors = SenseeBadgeDefaults.dangerColors())
                SenseeBadge(text = "Info", colors = SenseeBadgeDefaults.infoColors())
            }
        }
    }

@Preview
@Composable
private fun SenseeBadgeDarkPreview() =
    SenseePreview(themeMode = SenseeThemeMode.Dark) {
        Row(horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
            SenseeBadge(text = "Neutral")
            SenseeBadge(text = "Accent", colors = SenseeBadgeDefaults.accentColors())
            SenseeBadge(text = "Success", colors = SenseeBadgeDefaults.successColors())
            SenseeBadge(text = "Danger", colors = SenseeBadgeDefaults.dangerColors())
        }
    }
