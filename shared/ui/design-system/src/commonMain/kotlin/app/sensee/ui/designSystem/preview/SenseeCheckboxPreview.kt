package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.checkbox.SenseeCheckbox
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.SenseeThemeMode

@Preview
@Composable
private fun SenseeCheckboxPreview() =
    SenseePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
            SenseeCheckbox(checked = false)
            SenseeCheckbox(checked = true)
        }
    }

@Preview
@Composable
private fun SenseeCheckboxDarkPreview() =
    SenseePreview(themeMode = SenseeThemeMode.Dark) {
        Row(horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
            SenseeCheckbox(checked = false)
            SenseeCheckbox(checked = true)
        }
    }
