package app.sensee.feature.profile.presentation.impl.learningsettings.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback

internal object ProfileTopicPickerTextKeys {
    val Title = TextKey("profile.topicPicker.title")
    val Loading = TextKey("profile.topicPicker.loading")
    val LoadError = TextKey("profile.topicPicker.load_error")
    val Close = TextKey("profile.topicPicker.close")
}

internal val DefaultProfileTopicPickerTextProvider =
    MapTextProvider(
        mapOf(
            ProfileTopicPickerTextKeys.Title to "Темы для примеров",
            ProfileTopicPickerTextKeys.Loading to "Загружаем темы",
            ProfileTopicPickerTextKeys.LoadError to "Не удалось загрузить темы",
            ProfileTopicPickerTextKeys.Close to "Закрыть",
        ),
    )

@Composable
internal fun rememberProfileTopicPickerTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultProfileTopicPickerTextProvider.withFallback(parent) }
}
