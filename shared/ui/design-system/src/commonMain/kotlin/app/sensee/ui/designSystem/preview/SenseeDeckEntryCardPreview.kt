package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.deckEntryCard.SenseeDeckEntryCard
import app.sensee.ui.designSystem.icons.LibraryBooks24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Preview
@Composable
private fun SenseeDeckEntryCardPreview() =
    SenseePreview {
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium)) {
            SenseeDeckEntryCard(
                title = { Text(text = "Irregular verbs", style = SenseeTheme.typography.titleMedium) },
                subtitle = { Text(text = "42 cards · 12 due", style = SenseeTheme.typography.bodySmall) },
                leading = { SenseeIcon(imageVector = LibraryBooks24px, contentDescription = null) },
                meta = { SenseeBadge(text = "A2") },
                progress = 0.6f,
                onClick = {},
            )
            SenseeDeckEntryCard(
                title = { Text(text = "Phrasal verbs", style = SenseeTheme.typography.titleMedium) },
                subtitle = { Text(text = "Coming soon", style = SenseeTheme.typography.bodySmall) },
                enabled = false,
                onClick = {},
            )
        }
    }
