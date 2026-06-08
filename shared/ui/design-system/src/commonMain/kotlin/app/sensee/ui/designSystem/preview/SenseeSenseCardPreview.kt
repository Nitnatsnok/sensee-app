package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.badge.SenseeBadgeDefaults
import app.sensee.ui.designSystem.component.senseCard.SenseeSenseCard
import app.sensee.ui.designSystem.component.sentence.SenseeSentence
import app.sensee.ui.designSystem.component.sentence.SenseeSentencePart
import app.sensee.ui.designSystem.component.sentence.SenseeSentenceWordState
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.persistentListOf

@Preview
@Composable
private fun SenseeSenseCardPreview() =
    SenseePreview {
        val example =
            SenseeSentence(
                persistentListOf(
                    SenseeSentencePart.Text("I "),
                    SenseeSentencePart.Word(id = "t", value = "came across", state = SenseeSentenceWordState.Target),
                    SenseeSentencePart.Text(" an old photo."),
                ),
            )
        Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium)) {
            // Selectable (capture-style): accent border + corner checkmark + inline badge.
            SenseeSenseCard(
                title = { Text(text = "come across", style = SenseeTheme.typography.titleMedium) },
                translation = "наткнуться",
                explanation = "случайно обнаружить что-то",
                example = example,
                badge = { SenseeBadge(text = "phrasal verb", colors = SenseeBadgeDefaults.accentColors()) },
                selected = true,
                onClick = {},
            )
            // Read-only (Library-style): no checkmark slot, no border, no click.
            SenseeSenseCard(
                title = { Text(text = "deadline", style = SenseeTheme.typography.titleMedium) },
                translation = "крайний срок",
                explanation = "момент, к которому нужно завершить работу",
            )
        }
    }
