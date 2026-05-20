package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.sensee.feature.practice.presentation.api.DeckPracticeUiState
import app.sensee.feature.practice.presentation.impl.text.DefaultPracticeTextProvider
import app.sensee.ui.designSystem.preview.SenseePreview

private class DeckPracticeUiStateProvider : PreviewParameterProvider<DeckPracticeUiState> {
    override val values: Sequence<DeckPracticeUiState> =
        sequenceOf(
            DeckPracticePreviewData.loading,
            DeckPracticePreviewData.error,
            DeckPracticePreviewData.content,
            DeckPracticePreviewData.finished,
        )
}

@Preview(widthDp = 360, heightDp = 720)
@Composable
private fun DeckPracticePanePreview(
    @PreviewParameter(DeckPracticeUiStateProvider::class) uiState: DeckPracticeUiState,
) = SenseePreview(modifier = Modifier.fillMaxSize()) {
    DeckPracticePane(
        uiState = uiState,
        onAction = {},
        onHelpRequest = {},
        textProvider = DefaultPracticeTextProvider,
        modifier = Modifier.fillMaxSize(),
    )
}
