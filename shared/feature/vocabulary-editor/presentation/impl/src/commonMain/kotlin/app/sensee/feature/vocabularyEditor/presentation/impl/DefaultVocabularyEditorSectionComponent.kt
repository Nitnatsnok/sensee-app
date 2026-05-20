package app.sensee.feature.vocabularyEditor.presentation.impl

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureComponent
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyEditorSectionComponent
import app.sensee.feature.vocabularyEditor.presentation.navigationApi.VocabularyEditorConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@AssistedInject
public class DefaultVocabularyEditorSectionComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted target: VocabularyEditorConfig?,
    captureComponentFactory: VocabularyCaptureComponent.Factory,
) : VocabularyEditorSectionComponent,
    AppComponentContext by componentContext {
    override val capture: VocabularyCaptureComponent = captureComponentFactory.create(componentContext)

    private val configState = MutableStateFlow(target ?: VocabularyEditorConfig.QuickCapture)
    private val showBottomBarState = MutableStateFlow(false)

    override val showBottomBar: StateFlow<Boolean> = showBottomBarState.asStateFlow()

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus {
        if (!applyConfig(target)) {
            return NavigationRequestStatus.Unhandled
        }
        onComplete(true)
        return NavigationRequestStatus.Handled
    }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        onResult(NavigationRequestStatus.Unhandled)
    }

    private fun applyConfig(target: ScreenConfig): Boolean {
        if (target !is VocabularyEditorConfig) return false
        configState.update { target }
        showBottomBarState.update { false }
        return true
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<VocabularyEditorSectionComponent.Factory>(),
    )
    public fun interface Factory : VocabularyEditorSectionComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            target: VocabularyEditorConfig?,
        ): DefaultVocabularyEditorSectionComponent
    }
}
