package app.sensee.feature.vocabularyEditor.presentation.impl.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureAction
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureComponent
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureUiState
import app.sensee.feature.vocabularyEditor.presentation.impl.screen.VocabularyCaptureTextKeys
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeInlineStatus
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import com.composeunstyled.Text

/** Inline load-status row for the grammar-label dictionary; collapses on Idle/Success. */
@Composable
internal fun GrammarLabelsLoadStatus(
    state: DataLoadingState,
    onRetry: () -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    when (state) {
        DataLoadingState.Loading ->
            SenseeInlineStatus(
                modifier = modifier,
                text = textProvider.text(VocabularyCaptureTextKeys.GrammarLabelsLoading),
            )
        is DataLoadingState.Error ->
            SenseeInlineStatus(
                modifier = modifier,
                text = textProvider.text(VocabularyCaptureTextKeys.GrammarLabelsError),
                actions = {
                    SenseeButton(onClick = onRetry) {
                        Text(text = textProvider.text(VocabularyCaptureTextKeys.GrammarLabelsRetry))
                    }
                },
            )
        DataLoadingState.Idle,
        DataLoadingState.Success,
        -> Unit
    }
}

internal fun LazyListScope.grammarLabelsLoadStatusItem(
    uiState: VocabularyCaptureUiState,
    component: VocabularyCaptureComponent,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    val state = uiState.grammarLabelsState
    if (state == DataLoadingState.Idle || state == DataLoadingState.Success) return
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            GrammarLabelsLoadStatus(
                state = state,
                onRetry = { component.onAction(VocabularyCaptureAction.RetryGrammarLabels) },
                textProvider = textProvider,
            )
        }
    }
}
