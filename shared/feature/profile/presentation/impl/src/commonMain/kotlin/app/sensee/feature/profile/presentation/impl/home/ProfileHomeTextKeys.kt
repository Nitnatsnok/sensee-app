package app.sensee.feature.profile.presentation.impl.home

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig

internal object ProfileHomeTextKeys {
    val GroupSettings = TextKey("profile.home.group.settings")
    val CategoryApp = TextKey("profile.home.category.app")
    val CategoryLearning = TextKey("profile.home.category.learning")
    val CategoryPractice = TextKey("profile.home.category.practice")
    val CategoryAi = TextKey("profile.home.category.ai")
    val CategoryExperimental = TextKey("profile.home.category.experimental")
    val PlaceholderDescription = TextKey("profile.home.placeholder.description")
    val Back = TextKey("profile.home.back")
    val Close = TextKey("profile.home.close")
}

internal fun ProfileConfig.Settings.categoryTitleKey(): TextKey =
    when (this) {
        ProfileConfig.Settings.App -> ProfileHomeTextKeys.CategoryApp
        ProfileConfig.Settings.Learning -> ProfileHomeTextKeys.CategoryLearning
        ProfileConfig.Settings.Practice -> ProfileHomeTextKeys.CategoryPractice
        ProfileConfig.Settings.Ai -> ProfileHomeTextKeys.CategoryAi
        ProfileConfig.Settings.Experimental -> ProfileHomeTextKeys.CategoryExperimental
    }

internal val DefaultProfileHomeTextProvider =
    MapTextProvider(
        mapOf(
            ProfileHomeTextKeys.GroupSettings to "Настройки",
            ProfileHomeTextKeys.CategoryApp to "Приложение",
            ProfileHomeTextKeys.CategoryLearning to "Обучение",
            ProfileHomeTextKeys.CategoryPractice to "Практика",
            ProfileHomeTextKeys.CategoryAi to "ИИ и озвучивание",
            ProfileHomeTextKeys.CategoryExperimental to "Экспериментальные",
            ProfileHomeTextKeys.PlaceholderDescription to "Этот раздел настроек появится позже.",
            ProfileHomeTextKeys.Back to "Назад",
            ProfileHomeTextKeys.Close to "Закрыть",
        ),
    )
