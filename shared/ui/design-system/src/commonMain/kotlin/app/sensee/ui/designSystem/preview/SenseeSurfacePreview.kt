package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Preview
@Composable
private fun SenseeSurfacePreview() =
    SenseePreview {
        SenseeSurface {
            Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
                Text(text = "Surface panel", style = SenseeTheme.typography.titleMedium)
                Text(
                    text = "Bordered container primitive used for grouped sections.",
                    style = SenseeTheme.typography.bodyMedium,
                )
            }
        }
    }
