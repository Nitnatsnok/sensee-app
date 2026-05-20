package app.sensee.feature.vocabularyEditor.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

@Serializable
public sealed interface VocabularyEditorConfig : ScreenConfig {
    /** Zero-friction inbox capture: term + language, then AI-assisted refinement. */
    @Serializable
    public data object QuickCapture : VocabularyEditorConfig

    @Serializable
    public data class Editor(
        val entryId: String? = null,
    ) : VocabularyEditorConfig
}
