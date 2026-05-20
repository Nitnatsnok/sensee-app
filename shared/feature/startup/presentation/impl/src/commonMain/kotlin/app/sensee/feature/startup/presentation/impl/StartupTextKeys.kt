package app.sensee.feature.startup.presentation.impl

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey

internal object StartupTextKeys {
    val AppName = TextKey("startup.app_name")
    val StatusStarting = TextKey("startup.status_starting")
}

internal val DefaultStartupTextProvider =
    MapTextProvider(
        mapOf(
            StartupTextKeys.AppName to "Sensee",
            StartupTextKeys.StatusStarting to "Запуск",
        ),
    )
