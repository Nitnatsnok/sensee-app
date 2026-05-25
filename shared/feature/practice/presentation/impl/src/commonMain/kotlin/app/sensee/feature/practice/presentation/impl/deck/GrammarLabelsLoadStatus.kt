package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.impl.text.PracticeTextKeys
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeInlineStatus
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
                text = textProvider.text(PracticeTextKeys.GrammarLabelsLoading),
            )
        is DataLoadingState.Error ->
            SenseeInlineStatus(
                modifier = modifier,
                text = textProvider.text(PracticeTextKeys.GrammarLabelsError),
                actions = {
                    SenseeButton(onClick = onRetry) {
                        Text(text = textProvider.text(PracticeTextKeys.GrammarLabelsRetry))
                    }
                },
            )
        DataLoadingState.Idle,
        DataLoadingState.Success,
        -> Unit
    }
}
