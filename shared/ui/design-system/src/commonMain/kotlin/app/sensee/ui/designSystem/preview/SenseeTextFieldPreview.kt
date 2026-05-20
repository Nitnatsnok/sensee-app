package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.textField.SenseeTextField
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Preview
@Composable
private fun SenseeTextFieldPreview() =
    SenseePreview {
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium)) {
            SenseeTextField(
                state = rememberTextFieldState(initialText = "to run"),
                label = { Text("Word") },
            )
            SenseeTextField(
                state = rememberTextFieldState(),
                label = { Text("Translation") },
                placeholder = { Text("Enter translation") },
            )
            SenseeTextField(
                state = rememberTextFieldState(initialText = "rnu"),
                label = { Text("Word") },
                isError = true,
                supportingText = { Text("Unknown word") },
            )
            SenseeTextField(
                state = rememberTextFieldState(initialText = "Disabled"),
                label = { Text("Word") },
                enabled = false,
            )
            SenseeTextField(
                state = rememberTextFieldState(initialText = "sk-secret-key"),
                label = { Text("API key") },
                secure = true,
            )
        }
    }
