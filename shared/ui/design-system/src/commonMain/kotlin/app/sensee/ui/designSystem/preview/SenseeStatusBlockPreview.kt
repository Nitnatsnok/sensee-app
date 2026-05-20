package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeEmptyState
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import com.composeunstyled.Text

@Preview
@Composable
private fun SenseeLoadingStatePreview() =
    SenseePreview {
        Box(modifier = Modifier.height(200.dp)) {
            SenseeLoadingState(title = "Loading decks…")
        }
    }

@Preview
@Composable
private fun SenseeEmptyStatePreview() =
    SenseePreview {
        Box(modifier = Modifier.height(280.dp)) {
            SenseeEmptyState(
                title = "No decks yet",
                description = "Create your first deck to start practicing.",
                actions = { SenseeButton(onClick = {}) { Text("Create a deck") } },
            )
        }
    }

@Preview
@Composable
private fun SenseeErrorStatePreview() =
    SenseePreview {
        Box(modifier = Modifier.height(280.dp)) {
            SenseeErrorState(
                title = "Something went wrong",
                description = "We couldn't load your library.",
                actions = { SenseeButton(onClick = {}) { Text("Retry") } },
            )
        }
    }
