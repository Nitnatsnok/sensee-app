package app.sensee.feature.profile.presentation.impl.appsettings

import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.settings.domain.AppSettings
import app.sensee.settings.domain.AppThemeMode
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileAppSettingsLogicTest {
    private class FakeSettings(
        initial: UserSettingsSnapshot = UserSettingsSnapshot(),
    ) : UserSettingsRepository {
        private val flow = MutableStateFlow(initial)

        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flow.asStateFlow()

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = flow.value

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot {
            val next = transform(flow.value)
            flow.value = next
            return next
        }

        fun snapshot(): UserSettingsSnapshot = flow.value

        fun emitThemeMode(themeMode: AppThemeMode) {
            flow.value = flow.value.copy(app = flow.value.app.copy(themeMode = themeMode))
        }
    }

    private fun logic(settings: UserSettingsRepository): ProfileAppSettingsLogic =
        ProfileAppSettingsLogic(
            settingsRepository = settings,
            appDispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        )

    @Test
    fun `load reflects stored app theme mode`() {
        val stored =
            UserSettingsSnapshot(
                app = AppSettings(themeMode = AppThemeMode.Dark),
            )

        val state = logic(FakeSettings(stored)).uiState.value

        assertEquals(AppThemeMode.Dark, state.themeMode)
    }

    @Test
    fun `set theme mode persists app settings and updates state`() {
        val settings = FakeSettings()
        val logic = logic(settings)

        logic.setThemeMode(AppThemeMode.Light)

        assertEquals(AppThemeMode.Light, logic.uiState.value.themeMode)
        assertEquals(AppThemeMode.Light, settings.snapshot().app.themeMode)
    }

    @Test
    fun `external settings updates stay in sync`() {
        val settings = FakeSettings()
        val logic = logic(settings)

        settings.emitThemeMode(AppThemeMode.Dark)

        assertEquals(AppThemeMode.Dark, logic.uiState.value.themeMode)
    }
}
