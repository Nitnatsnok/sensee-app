package app.sensee.feature.profile.presentation.impl.learningsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback

internal object ProfileLearningSettingsTextKeys {
    val SectionTopics = TextKey("profile.learningSettings.section.topics")
    val TopicsHint = TextKey("profile.learningSettings.topics_hint")
    val TopicsLoading = TextKey("profile.learningSettings.topics_loading")
    val TopicsLoadError = TextKey("profile.learningSettings.topics_load_error")
    val PickerEmpty = TextKey("profile.learningSettings.picker_empty")
    val PickerSelectedPrefix = TextKey("profile.learningSettings.picker_selected_prefix")
    val PickerMore = TextKey("profile.learningSettings.picker_more")
}

internal val DefaultProfileLearningSettingsTextProvider =
    MapTextProvider(
        mapOf(
            ProfileLearningSettingsTextKeys.SectionTopics to "Темы для примеров",
            ProfileLearningSettingsTextKeys.TopicsHint to
                "AI-ассистент будет подбирать примеры предложений по выбранным темам там, где это " +
                "естественно для значения слова. Можно ничего не отмечать — тогда тематический уклон не применяется.",
            ProfileLearningSettingsTextKeys.TopicsLoading to "Загружаем темы",
            ProfileLearningSettingsTextKeys.TopicsLoadError to "Не удалось загрузить темы",
            ProfileLearningSettingsTextKeys.PickerEmpty to "Не выбрано",
            ProfileLearningSettingsTextKeys.PickerSelectedPrefix to "Выбрано:",
            ProfileLearningSettingsTextKeys.PickerMore to "ещё",
        ),
    )

@Composable
internal fun rememberProfileLearningSettingsTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultProfileLearningSettingsTextProvider.withFallback(parent) }
}
