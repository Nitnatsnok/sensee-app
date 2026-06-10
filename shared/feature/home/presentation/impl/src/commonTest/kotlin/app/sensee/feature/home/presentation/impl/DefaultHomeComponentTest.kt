package app.sensee.feature.home.presentation.impl

import app.sensee.core.decompose.context.RootComponentContext
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.home.presentation.api.HomeAction
import app.sensee.feature.practice.domain.DuePracticeRepository
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import app.sensee.srs.core.id.SrsCardId
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.PolymorphicSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultHomeComponentTest {
    @Test
    fun `returning to Home re-reads the due count as of now`() {
        val clock = AdvanceableClock(Instant.fromEpochMilliseconds(0L))
        val lifecycle = LifecycleRegistry()
        val component =
            buildComponent(
                lifecycle = lifecycle,
                clock = clock,
                due = DueFromInstant(dueFrom = Instant.fromEpochMilliseconds(1_000L)),
            )
        lifecycle.resume()
        assertEquals(0, component.uiState.value.dueCount, "the card is not due yet at t=0")

        // Time passes (the card falls due) and the user comes back to Home.
        clock.instant = Instant.fromEpochMilliseconds(1_000L)
        lifecycle.pause()
        lifecycle.resume()

        assertEquals(
            1,
            component.uiState.value.dueCount,
            "resume re-evaluates now, so the card that just fell due is counted",
        )
    }

    @Test
    fun `starting a due session opens the due practice config`() {
        val navigation = RecordingNavigation()
        val component =
            buildComponent(
                lifecycle = LifecycleRegistry(),
                clock = AdvanceableClock(Instant.fromEpochMilliseconds(0L)),
                due = DueFromInstant(dueFrom = Instant.fromEpochMilliseconds(0L)),
                navigation = navigation,
            )

        component.onAction(HomeAction.StartDueSession)

        assertEquals(PracticeConfig.DuePractice, navigation.opened, "the CTA opens the due practice session")
    }

    private fun buildComponent(
        lifecycle: LifecycleRegistry,
        clock: Clock,
        due: DuePracticeRepository,
        navigation: NavigationDispatcher = NoOpNavigation,
    ): DefaultHomeComponent {
        val componentContext =
            RootComponentContext(
                delegate = DefaultComponentContext(lifecycle),
                screenConfigSerializer = PolymorphicSerializer(ScreenConfig::class),
                navigation = navigation,
            )
        val logicFactory =
            HomeLogic.Factory {
                HomeLogic(
                    duePracticeRepository = due,
                    userSettingsRepository = FakeSettings(),
                    clock = clock,
                    appDispatchers = immediateAppDispatchers(),
                    appDiagnostics = noOpAppDiagnostics(),
                )
            }
        return DefaultHomeComponent(
            componentContext = componentContext,
            homeLogicFactory = logicFactory,
        )
    }

    private object NoOpNavigation : NavigationDispatcher {
        override fun open(
            target: ScreenConfig,
            onComplete: (isSuccess: Boolean) -> Unit,
        ): NavigationRequestStatus {
            onComplete(false)
            return NavigationRequestStatus.Unhandled
        }

        override fun back(onResult: (NavigationRequestStatus) -> Unit) = Unit
    }

    private class RecordingNavigation : NavigationDispatcher {
        var opened: ScreenConfig? = null
            private set

        override fun open(
            target: ScreenConfig,
            onComplete: (isSuccess: Boolean) -> Unit,
        ): NavigationRequestStatus {
            opened = target
            onComplete(true)
            return NavigationRequestStatus.Handled
        }

        override fun back(onResult: (NavigationRequestStatus) -> Unit) = Unit
    }

    private class AdvanceableClock(
        var instant: Instant,
    ) : Clock {
        override fun now(): Instant = instant
    }

    // Due-count as a function of `now`: a single card falls due at [dueFrom].
    private class DueFromInstant(
        private val dueFrom: Instant,
    ) : DuePracticeRepository {
        private fun countAt(now: Instant): Int = if (now >= dueFrom) 1 else 0

        override suspend fun countDue(now: Instant): Int = countAt(now)

        override fun observeDueCount(now: Instant): Flow<Int> = flowOf(countAt(now))

        override suspend fun dueCardIds(
            now: Instant,
            limit: Int,
        ): List<SrsCardId> = emptyList()
    }

    private class FakeSettings : UserSettingsRepository {
        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> =
            flowOf(UserSettingsSnapshot())

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = UserSettingsSnapshot()

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = UserSettingsSnapshot()
    }
}
