package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.selectField.SenseeSelectField
import app.sensee.ui.designSystem.theme.SenseeTheme
import kotlinx.collections.immutable.persistentListOf

@Preview
@Composable
private fun SenseeSelectFieldPreview() =
    SenseePreview {
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium)) {
            var selected by remember { mutableStateOf<String?>("gpt-4o-mini") }
            SenseeSelectField(
                label = "AI model",
                selected = selected,
                options = persistentListOf("gpt-4o-mini", "gpt-4o", "o3-mini"),
                optionLabel = { it },
                onSelect = { selected = it },
            )
            SenseeSelectField(
                label = "Voice",
                selected = null,
                options = persistentListOf("alloy", "echo", "nova"),
                optionLabel = { it },
                onSelect = {},
                placeholder = "Choose a voice",
            )
        }
    }
