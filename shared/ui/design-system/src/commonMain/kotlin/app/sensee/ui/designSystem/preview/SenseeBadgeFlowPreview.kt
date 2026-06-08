package app.sensee.ui.designSystem.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.badge.SenseeBadgeDefaults
import app.sensee.ui.designSystem.component.badge.SenseeBadgeFlow
import kotlinx.collections.immutable.persistentListOf

@Preview
@Composable
private fun SenseeBadgeFlowPreview() =
    SenseePreview {
        SenseeBadgeFlow(
            labels = persistentListOf("transitive", "+ object", "+ preposition", "formal", "current", "phrasal verb"),
            colors = SenseeBadgeDefaults.infoColors(),
        )
    }
