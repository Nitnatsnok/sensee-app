package app.sensee.feature.practice.presentation.impl.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.sensee.feature.practice.presentation.api.PracticeHomeUiState
import app.sensee.feature.practice.presentation.impl.text.DefaultPracticeTextProvider
import app.sensee.ui.designSystem.preview.SenseePreview

private class PracticeHomeUiStateProvider : PreviewParameterProvider<PracticeHomeUiState> {
    override val values: Sequence<PracticeHomeUiState> =
        sequenceOf(
            PracticeHomePreviewData.loading,
            PracticeHomePreviewData.error,
            PracticeHomePreviewData.empty,
            PracticeHomePreviewData.content,
        )
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun PracticeHomeContentPreview(
    @PreviewParameter(PracticeHomeUiStateProvider::class) uiState: PracticeHomeUiState,
) = SenseePreview(modifier = Modifier.fillMaxSize()) {
    PracticeHomeContent(
        uiState = uiState,
        onAction = {},
        textProvider = DefaultPracticeTextProvider,
        modifier = Modifier.fillMaxSize(),
    )
}
