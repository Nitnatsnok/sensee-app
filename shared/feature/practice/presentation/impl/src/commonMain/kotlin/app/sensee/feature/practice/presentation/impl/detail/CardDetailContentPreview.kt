package app.sensee.feature.practice.presentation.impl.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.sensee.feature.practice.presentation.api.CardDetailUiState
import app.sensee.feature.practice.presentation.impl.text.DefaultPracticeTextProvider
import app.sensee.ui.designSystem.preview.SenseePreview

private class CardDetailUiStateProvider : PreviewParameterProvider<CardDetailUiState> {
    override val values: Sequence<CardDetailUiState> =
        sequenceOf(
            CardDetailPreviewData.loading,
            CardDetailPreviewData.error,
            CardDetailPreviewData.empty,
            CardDetailPreviewData.content,
            CardDetailPreviewData.contentWithRelated,
        )
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun CardDetailContentPreview(
    @PreviewParameter(CardDetailUiStateProvider::class) uiState: CardDetailUiState,
) = SenseePreview(modifier = Modifier.fillMaxSize()) {
    CardDetailContent(
        uiState = uiState,
        onAction = {},
        modifier = Modifier.fillMaxSize(),
        textProvider = DefaultPracticeTextProvider,
    )
}
