package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.topBar.SenseeImmersiveTopBar
import app.sensee.ui.designSystem.component.topBar.SenseeTopBar
import app.sensee.ui.designSystem.component.topBar.SenseeTopBarIconButton
import app.sensee.ui.designSystem.icons.ArrowBack24px
import app.sensee.ui.designSystem.icons.Close24px
import app.sensee.ui.designSystem.icons.Help24px
import app.sensee.ui.designSystem.icons.Settings24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Preview
@Composable
private fun SenseeTopBarPreview() =
    SenseePreview {
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.large)) {
            SenseeTopBar(
                title = { Text(text = "Library", style = SenseeTheme.typography.titleLarge) },
                navigation = {
                    SenseeTopBarIconButton(
                        onClick = {},
                        icon = { SenseeIcon(imageVector = ArrowBack24px, contentDescription = "Back") },
                    )
                },
                actions = {
                    SenseeTopBarIconButton(
                        onClick = {},
                        icon = { SenseeIcon(imageVector = Settings24px, contentDescription = "Settings") },
                    )
                },
            )
            SenseeImmersiveTopBar(
                navigation = {
                    SenseeTopBarIconButton(
                        onClick = {},
                        icon = { SenseeIcon(imageVector = Close24px, contentDescription = "Close") },
                    )
                },
                actions = {
                    SenseeTopBarIconButton(
                        onClick = {},
                        icon = { SenseeIcon(imageVector = Help24px, contentDescription = "Help") },
                    )
                },
            )
        }
    }
