package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.component.UnimplementedScreen

@Preview
@Composable
private fun UnimplementedScreenPreview() =
    SenseePreview {
        Box(modifier = Modifier.height(360.dp)) {
            UnimplementedScreen(
                title = "Profile",
                description = "This screen is not implemented yet.",
            )
        }
    }
