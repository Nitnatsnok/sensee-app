package app.sensee.feature.vocabularyEditor.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.navigation.BottomBarVisibilityOwner
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.feature.vocabularyEditor.presentation.navigationApi.VocabularyEditorConfig

public interface VocabularyEditorSectionComponent :
    AppComponent,
    NavigationDispatcher,
    BottomBarVisibilityOwner {
    public val capture: VocabularyCaptureComponent

    public interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            target: VocabularyEditorConfig? = null,
        ): VocabularyEditorSectionComponent
    }
}
