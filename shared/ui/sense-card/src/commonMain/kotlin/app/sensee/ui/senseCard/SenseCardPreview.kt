package app.sensee.ui.senseCard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.lexicon.domain.Sense
import app.sensee.ui.designSystem.preview.SenseePreview

private data class SenseCardPreviewCase(
    val sense: Sense,
    val selected: Boolean = false,
    val expanded: Boolean = false,
    val selectable: Boolean = true,
)

private class SenseCardPreviewCaseProvider : PreviewParameterProvider<SenseCardPreviewCase> {
    override val values: Sequence<SenseCardPreviewCase> =
        sequenceOf(
            SenseCardPreviewCase(SenseCardPreviewData.richPhrasalVerb, selected = true),
            SenseCardPreviewCase(SenseCardPreviewData.richPhrasalVerb, expanded = true, selectable = false),
            SenseCardPreviewCase(SenseCardPreviewData.irregularVerb, selected = true, expanded = true),
            SenseCardPreviewCase(SenseCardPreviewData.minimal, selectable = false),
        )
}

@Preview(widthDp = 360)
@Composable
private fun SenseCardPreview(
    @PreviewParameter(SenseCardPreviewCaseProvider::class) case: SenseCardPreviewCase,
) = SenseePreview {
    SenseCard(
        sense = case.sense,
        labels = GrammarLabels.EMPTY,
        studyLanguageTag = "en",
        selected = case.selected,
        onClick = if (case.selectable) ({}) else null,
        initiallyExpanded = case.expanded,
        textProvider = DefaultSenseCardTextProvider,
    )
}
