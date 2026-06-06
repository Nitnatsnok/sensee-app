package app.sensee.feature.vocabularyEditor.presentation.impl.component

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureAction
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureComponent
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureUiState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultVocabularyCaptureComponent(
    @Assisted componentContext: AppComponentContext,
    private val captureLogicFactory: VocabularyCaptureLogic.Factory,
) : VocabularyCaptureComponent,
    AppComponentContext by componentContext {
    private val logic =
        getOrCreateLogic(LogicKey("VocabularyCaptureLogic")) {
            captureLogicFactory.create()
        }

    override val uiState: StateFlow<VocabularyCaptureUiState> = logic.uiState

    override fun onAction(action: VocabularyCaptureAction) {
        when (action) {
            is VocabularyCaptureAction.Suggest -> logic.suggest(action.term)
            is VocabularyCaptureAction.ToggleCandidate -> logic.toggleCandidate(action.contentKey)
            is VocabularyCaptureAction.AddManual ->
                logic.addManual(action.translation, action.surfaceForm, action.unitType, action.example)
            is VocabularyCaptureAction.CompleteManualWithAssistant ->
                logic.completeManualWithAssistant(action.manualIndex)
            is VocabularyCaptureAction.ToggleManualSuggestion ->
                logic.toggleManualSuggestion(action.manualIndex, action.suggestionContentKey)
            VocabularyCaptureAction.ConfirmSelected -> logic.confirmSelected()
            VocabularyCaptureAction.Reset -> logic.reset()
            VocabularyCaptureAction.RetryGrammarLabels -> logic.retryGrammarLabels()
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<VocabularyCaptureComponent.Factory>(),
    )
    public fun interface Factory : VocabularyCaptureComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultVocabularyCaptureComponent
    }
}
