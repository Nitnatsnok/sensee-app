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

internal fun ProfileConfig.categoryTitleKey(): TextKey =
    when (this) {
        ProfileConfig.AppSettings -> ProfileHomeTextKeys.CategoryApp
        ProfileConfig.LearningSettings -> ProfileHomeTextKeys.CategoryLearning
        ProfileConfig.PracticeSettings -> ProfileHomeTextKeys.CategoryPractice
        ProfileConfig.AiSettings -> ProfileHomeTextKeys.CategoryAi
        ProfileConfig.ExperimentalSettings -> ProfileHomeTextKeys.CategoryExperimental
        ProfileConfig.Home -> error("Profile Home is the menu itself, not a category row: $this")
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
