package app.sensee.feature.home.presentation.impl

import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.practice.domain.DuePracticeRepository
import app.sensee.feature.practice.domain.PracticeSessionPolicy
import app.sensee.settings.domain.PracticeSettings
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import app.sensee.srs.core.id.SrsCardId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class HomeLogicTest {
    @Test
    fun `home combines due count and daily goal into ready state`() {
        val logic = newLogic(FakeDue(dueCount = 5), FakeSettings(dailyGoal = 30))

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals(5, state.dueCount)
        assertEquals(30, state.dailyGoal)
        assertTrue(state.isReviewActionable, "with cards due the review CTA is actionable")
    }

    @Test
    fun `home caps the due count to the session limit and flags the overflow`() {
        val overflow = PracticeSessionPolicy.DUE_SESSION_LIMIT + 5
        val logic = newLogic(FakeDue(dueCount = overflow), FakeSettings())

        val state = logic.uiState.value
        assertEquals(PracticeSessionPolicy.DUE_SESSION_LIMIT, state.dueCount, "shown count never exceeds one session")
        assertTrue(state.dueExceedsSessionLimit, "more cards are due than one session takes")
    }

    @Test
    fun `home does not flag overflow when the due count fits one session`() {
        val logic = newLogic(FakeDue(dueCount = PracticeSessionPolicy.DUE_SESSION_LIMIT), FakeSettings())

        val state = logic.uiState.value
        assertEquals(PracticeSessionPolicy.DUE_SESSION_LIMIT, state.dueCount)
        assertTrue(!state.dueExceedsSessionLimit, "exactly the limit is not an overflow")
    }

    @Test
    fun `home reflects an updated due count from the stream`() {
        val due = FakeDue(dueCount = 0)
        val logic = newLogic(due, FakeSettings())
        assertEquals(0, logic.uiState.value.dueCount)
        assertTrue(!logic.uiState.value.isReviewActionable, "nothing due yet")

        due.emit(3)

        assertEquals(3, logic.uiState.value.dueCount)
        assertTrue(logic.uiState.value.isReviewActionable)
    }

    @Test
    fun `home surfaces an error when the aggregate stream fails`() {
        val logic = newLogic(FakeDue(failStream = true), FakeSettings())

        assertIs<DataLoadingState.Error>(logic.uiState.value.loadingState)
    }

    @Test
    fun `home recovers to a ready state when refresh re-subscribes after an error`() {
        val due = FakeDue(failStream = true)
        val logic = newLogic(due, FakeSettings(dailyGoal = 20))
        assertIs<DataLoadingState.Error>(logic.uiState.value.loadingState)

        due.recover(dueCount = 4)
        logic.refresh()

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals(4, state.dueCount)
    }

    @Test
    fun `home defaults the daily goal when settings are unset`() {
        val logic = newLogic(FakeDue(dueCount = 1), FakeSettings())

        assertEquals(PracticeSettings().dailyGoal, logic.uiState.value.dailyGoal)
    }

    // HomeLogic subscribes on appearance (Home resume), not in init, so the tests start
    // observation with an explicit refresh() — the "screen appeared" trigger. No lifecycle
    // scaffolding here; the resume wiring itself is covered by DefaultHomeComponentTest.
    private fun newLogic(
        due: FakeDue,
        settings: FakeSettings,
    ): HomeLogic =
        HomeLogic(
            duePracticeRepository = due,
            userSettingsRepository = settings,
            clock = FixedClock,
            appDispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        ).also { it.refresh() }

    private class FakeDue(
        dueCount: Int = 0,
        private var failStream: Boolean = false,
    ) : DuePracticeRepository {
        private val count = MutableStateFlow(dueCount)

        fun emit(value: Int) {
            count.value = value
        }

        // Heal the stream so a later re-subscribe (refresh) reads a working flow.
        fun recover(dueCount: Int) {
            failStream = false
            count.value = dueCount
        }

        override suspend fun countDue(now: Instant): Int = count.value

        override fun observeDueCount(now: Instant): Flow<Int> =
            if (failStream) {
                flow { throw IllegalStateException("due stream failed") }
            } else {
                count.asStateFlow()
            }

        override suspend fun dueCardIds(
            now: Instant,
            limit: Int,
        ): List<SrsCardId> = emptyList()
    }

    private class FakeSettings(
        dailyGoal: Int = PracticeSettings().dailyGoal,
    ) : UserSettingsRepository {
        private val settings =
            MutableStateFlow(UserSettingsSnapshot(practice = PracticeSettings(dailyGoal = dailyGoal)))

        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = settings.asStateFlow()

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = settings.value

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot {
            settings.value = transform(settings.value)
            return settings.value
        }
    }

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(0L)
    }
}
