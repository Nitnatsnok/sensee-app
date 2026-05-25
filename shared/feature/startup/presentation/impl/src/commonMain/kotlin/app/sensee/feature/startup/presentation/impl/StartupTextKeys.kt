package app.sensee.feature.startup.presentation.impl

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey

internal object StartupTextKeys {
    val AppName = TextKey("startup.app_name")
    val StatusStarting = TextKey("startup.status_starting")
    val ErrorTitle = TextKey("startup.error_title")
    val ErrorDescription = TextKey("startup.error_description")
    val Retry = TextKey("startup.retry")
}

internal val DefaultStartupTextProvider =
    MapTextProvider(
        mapOf(
            StartupTextKeys.AppName to "Sensee",
            StartupTextKeys.StatusStarting to "Запуск",
            StartupTextKeys.ErrorTitle to "Не удалось загрузить данные",
            StartupTextKeys.ErrorDescription to
                "Проверьте подключение к сети и повторите запуск.",
            StartupTextKeys.Retry to "Повторить",
        ),
    )
