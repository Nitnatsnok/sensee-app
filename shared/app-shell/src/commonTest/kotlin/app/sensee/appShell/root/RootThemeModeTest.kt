package app.sensee.appShell.root

import app.sensee.settings.domain.AppSettings
import app.sensee.settings.domain.AppThemeMode
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import app.sensee.ui.designSystem.theme.SenseeThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RootThemeModeTest {
    private class FakeSettings(
        private vararg val snapshots: UserSettingsSnapshot,
    ) : UserSettingsRepository {
        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(*snapshots)

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = snapshots.last()

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = transform(snapshots.last())
    }

    @Test
    fun `settings theme mode drives root theme mode`() =
        runTest {
            val settings =
                FakeSettings(
                    snapshot(AppThemeMode.System),
                    snapshot(AppThemeMode.Light),
                    snapshot(AppThemeMode.Dark),
                )
            val observed = settings.observeThemeMode().toList()

            assertEquals(
                listOf(SenseeThemeMode.System, SenseeThemeMode.Light, SenseeThemeMode.Dark),
                observed,
            )
        }

    @Test
    fun `haptic feedback enabled defaults to true`() =
        runTest {
            val settings = FakeSettings(UserSettingsSnapshot())

            assertTrue(settings.observeHapticFeedbackEnabled().toList().last())
        }

    @Test
    fun `settings haptic preference drives root haptic flag and dedups`() =
        runTest {
            val settings =
                FakeSettings(
                    hapticSnapshot(enabled = true),
                    hapticSnapshot(enabled = true),
                    hapticSnapshot(enabled = false),
                )

            val observed = settings.observeHapticFeedbackEnabled().toList()

            assertEquals(listOf(true, false), observed)
        }

    private companion object {
        fun snapshot(themeMode: AppThemeMode): UserSettingsSnapshot =
            UserSettingsSnapshot(app = AppSettings(themeMode = themeMode))

        fun hapticSnapshot(enabled: Boolean): UserSettingsSnapshot =
            UserSettingsSnapshot(app = AppSettings(hapticFeedbackEnabled = enabled))
    }
}
