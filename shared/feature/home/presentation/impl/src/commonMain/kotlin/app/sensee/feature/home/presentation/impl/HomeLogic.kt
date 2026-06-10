package app.sensee.feature.home.presentation.impl

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.home.presentation.api.HomeUiState
import app.sensee.feature.practice.domain.DuePracticeRepository
import app.sensee.feature.practice.domain.PracticeSessionPolicy
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/**
 * Aggregates the Home dashboard state from read-only seams it does not own: the due
 * count (`practice`) and the daily goal (`settings`). It schedules nothing and holds
 * no domain — the «Стоит повторить» CTA navigates out into `PracticeConfig.DuePractice`.
 */
@AssistedInject
public class HomeLogic(
    private val duePracticeRepository: DuePracticeRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val clock: Clock,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(): HomeLogic
    }

    private var observationJob: Job? = null

    public val uiState: StateFlow<HomeUiState>
        field = MutableStateFlow(HomeUiState())

    /**
     * Subscribe (or re-subscribe) the aggregate streams, reading "due as of now". There is no
     * eager subscription in init on purpose: the aggregates are anchored to a fixed [Clock.now]
     * at subscription, so the right moment to read is when Home appears, not when it is
     * constructed. The host drives this from Home resume (first appearance and every later
     * return to the foreground) and from the error Retry action — one read per appearance, with
     * no duplicate work and no read at a stale "construction" instant. Cheap today (local DB +
     * settings); keeping a single trigger keeps it correct if an aggregate later costs a network
     * call.
     */
    public fun refresh() {
        observe()
    }

    private fun observe() {
        observationJob?.cancel()
        observationJob =
            combine(
                // "Due" is measured against the moment the dashboard subscribes; the streams
                // re-emit as reviews and settings change. Re-checking the wall clock as time
                // passes is a Retry, not a tick — consistent with DuePracticeRepository's contract.
                duePracticeRepository.observeDueCount(clock.now()),
                userSettingsRepository.observeSettings().map { it.practice.dailyGoal },
            ) { dueCount, dailyGoal ->
                // Cap to the size of one due session so the widget never promises more than the
                // session delivers; the overflow is surfaced as "N+" rather than the raw backlog.
                HomeUiState(
                    loadingState = DataLoadingState.Success,
                    dueCount = dueCount.coerceAtMost(PracticeSessionPolicy.DUE_SESSION_LIMIT),
                    dueExceedsSessionLimit = dueCount > PracticeSessionPolicy.DUE_SESSION_LIMIT,
                    dailyGoal = dailyGoal,
                )
            }.onEach { state -> uiState.update { state } }
                .catch { throwable ->
                    logger.error(throwable) { "Home aggregates stream failed" }
                    crashReporter.recordException(
                        throwable,
                        attributes = mapOf("area" to "home", "operation" to "observe_home_aggregates"),
                    )
                    uiState.update { it.copy(loadingState = DataLoadingState.Error(throwable)) }
                }.launchIn(logicScope)
    }
}
