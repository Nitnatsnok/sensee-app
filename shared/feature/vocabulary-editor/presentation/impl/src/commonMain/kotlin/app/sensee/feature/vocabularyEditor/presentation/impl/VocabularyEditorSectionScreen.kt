package app.sensee.feature.vocabularyEditor.presentation.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyEditorSectionComponent

@Composable
public fun VocabularyEditorSectionScreen(
    component: VocabularyEditorSectionComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberVocabularyCaptureTextProvider(),
) {
    CompositionLocalProvider(LocalTextProvider provides textProvider) {
        VocabularyCaptureScreen(
            component = component.capture,
            modifier = modifier,
            textProvider = textProvider,
        )
    }
}

@Composable
internal fun rememberVocabularyCaptureTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultVocabularyCaptureTextProvider.withFallback(parent) }
}
