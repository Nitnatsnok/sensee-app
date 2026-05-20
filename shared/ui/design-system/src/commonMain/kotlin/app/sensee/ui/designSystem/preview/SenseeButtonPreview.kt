package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.button.SenseeButtonColors
import app.sensee.ui.designSystem.component.button.SenseeIconButton
import app.sensee.ui.designSystem.component.button.SenseeSpeakIconButton
import app.sensee.ui.designSystem.icons.Add24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.SenseeThemeMode
import com.composeunstyled.Text

@Preview
@Composable
private fun SenseeButtonVariantsPreview() =
    SenseePreview {
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
            SenseeButton(onClick = {}) { Text("Primary") }
            SenseeButton(onClick = {}, colors = SenseeButtonColors.tonal()) { Text("Tonal") }
            SenseeButton(onClick = {}, colors = SenseeButtonColors.outlined()) { Text("Outlined") }
            SenseeButton(onClick = {}, colors = SenseeButtonColors.text()) { Text("Text") }
            SenseeButton(onClick = {}, colors = SenseeButtonColors.danger()) { Text("Danger") }
            SenseeButton(onClick = {}, enabled = false) { Text("Disabled") }
        }
    }

@Preview
@Composable
private fun SenseeButtonVariantsDarkPreview() =
    SenseePreview(themeMode = SenseeThemeMode.Dark) {
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
            SenseeButton(onClick = {}) { Text("Primary") }
            SenseeButton(onClick = {}, colors = SenseeButtonColors.tonal()) { Text("Tonal") }
            SenseeButton(onClick = {}, colors = SenseeButtonColors.outlined()) { Text("Outlined") }
            SenseeButton(onClick = {}, enabled = false) { Text("Disabled") }
        }
    }

@Preview
@Composable
private fun SenseeIconButtonPreview() =
    SenseePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium)) {
            SenseeIconButton(
                onClick = {},
                icon = { SenseeIcon(imageVector = Add24px, contentDescription = "Add") },
            )
            SenseeIconButton(
                onClick = {},
                enabled = false,
                icon = { SenseeIcon(imageVector = Add24px, contentDescription = "Add") },
            )
            SenseeSpeakIconButton(onClick = {})
        }
    }
