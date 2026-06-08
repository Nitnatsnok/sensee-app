package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.badge.SenseeBadgeDefaults
import app.sensee.ui.designSystem.component.badge.SenseeBadgeFlow
import app.sensee.ui.designSystem.component.layout.SenseeLabeledSection
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.persistentListOf

@Preview
@Composable
private fun SenseeLabeledSectionPreview() =
    SenseePreview {
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium)) {
            SenseeLabeledSection(label = "Grammar") {
                SenseeBadgeFlow(
                    labels = persistentListOf("past simple", "irregular"),
                    colors = SenseeBadgeDefaults.infoColors(),
                )
            }
            SenseeLabeledSection(label = "Forms") {
                Text(text = "throw / threw / thrown", style = SenseeTheme.typography.bodyMedium)
            }
        }
    }
