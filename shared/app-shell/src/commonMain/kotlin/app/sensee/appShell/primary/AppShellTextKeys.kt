package app.sensee.appShell.primary

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey

internal object AppShellTextKeys {
    val PrimaryNavigationHome = TextKey("app_shell.primary_navigation.home")
    val PrimaryNavigationPractice = TextKey("app_shell.primary_navigation.practice")
    val PrimaryNavigationLibrary = TextKey("app_shell.primary_navigation.library")
    val PrimaryNavigationProfile = TextKey("app_shell.primary_navigation.profile")
    val PrimaryNavigationAddEntry = TextKey("app_shell.primary_navigation.add_entry")
}

internal val DefaultAppShellTextProvider =
    MapTextProvider(
        mapOf(
            AppShellTextKeys.PrimaryNavigationHome to "Главная",
            AppShellTextKeys.PrimaryNavigationPractice to "Практика",
            AppShellTextKeys.PrimaryNavigationLibrary to "Библиотека",
            AppShellTextKeys.PrimaryNavigationProfile to "Профиль",
            AppShellTextKeys.PrimaryNavigationAddEntry to "Добавить слово",
        ),
    )
