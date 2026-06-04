package app.sensee.feature.vocabularyEditor.presentation.impl.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.sensee.feature.vocabularyEditor.presentation.impl.screen.DefaultVocabularyCaptureTextProvider
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.lexicon.domain.Sense
import app.sensee.ui.designSystem.preview.SenseePreview

private data class SenseCardPreviewCase(
    val candidate: Sense,
    val selected: Boolean,
    val expanded: Boolean,
)

private class SenseCardPreviewCaseProvider : PreviewParameterProvider<SenseCardPreviewCase> {
    override val values: Sequence<SenseCardPreviewCase> =
        sequenceOf(
            SenseCardPreviewCase(SenseSelectionCardPreviewData.richPhrasalVerb, selected = false, expanded = false),
            SenseCardPreviewCase(SenseSelectionCardPreviewData.richPhrasalVerb, selected = true, expanded = false),
            SenseCardPreviewCase(SenseSelectionCardPreviewData.richPhrasalVerb, selected = false, expanded = true),
            SenseCardPreviewCase(SenseSelectionCardPreviewData.irregularVerb, selected = true, expanded = true),
            SenseCardPreviewCase(SenseSelectionCardPreviewData.minimal, selected = false, expanded = false),
        )
}

@Preview(widthDp = 360)
@Composable
private fun SenseSelectionCardPreview(
    @PreviewParameter(SenseCardPreviewCaseProvider::class) case: SenseCardPreviewCase,
) = SenseePreview {
    SenseSelectionCard(
        candidate = case.candidate,
        labels = GrammarLabels.EMPTY,
        studyLanguageTag = "en",
        textProvider = DefaultVocabularyCaptureTextProvider,
        selected = case.selected,
        onToggle = {},
        initiallyExpanded = case.expanded,
    )
}
