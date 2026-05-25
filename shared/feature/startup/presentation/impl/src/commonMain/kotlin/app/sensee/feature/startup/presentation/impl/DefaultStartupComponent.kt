package app.sensee.feature.startup.presentation.impl

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.tracing.Tracer
import app.sensee.core.tracing.span
import app.sensee.feature.startup.domain.PreloadAppStartupUseCase
import app.sensee.feature.startup.domain.PreloadOutcome
import app.sensee.feature.startup.presentation.api.StartupComponent
import app.sensee.feature.startup.presentation.api.StartupState
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State-machine [StartupComponent]: drives [state] from `Loading` to either `Loaded` or `Failed`. */
@AssistedInject
public class DefaultStartupComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onFinished: () -> Unit,
    private val preloadAppStartup: PreloadAppStartupUseCase,
    private val tracer: Tracer,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : StartupComponent,
    AppComponentContext by componentContext {
    private val componentScope: CoroutineScope =
        CoroutineScope(appDispatchers.main.immediate + SupervisorJob())

    private val logger: AppLogger = appDiagnostics.logger.tag("StartupComponent")

    private val mutableState = MutableStateFlow<StartupState>(StartupState.Loading)
    override val state: StateFlow<StartupState> = mutableState.asStateFlow()

    private var preloadJob: Job? = null

    init {
        startPreload()
        lifecycle.doOnDestroy { componentScope.cancel() }
    }

    override fun retry() {
        if (mutableState.value !is StartupState.Failed) return
        startPreload()
    }

    override fun onFinished() {
        onFinished.invoke()
    }

    private fun startPreload() {
        preloadJob?.cancel()
        mutableState.value = StartupState.Loading
        preloadJob =
            componentScope.launch {
                try {
                    val outcome =
                        tracer.span("startup.awaitReady") {
                            tracer.span("startup.preload") { preloadAppStartup() }
                        }
                    mutableState.value =
                        if (outcome.isFullSuccess) {
                            StartupState.Loaded
                        } else {
                            logger.warn {
                                "Startup preload partial — labels=${outcome.grammarLabelsLoaded}, " +
                                    "invariants=${outcome.taxonomyInvariantsLoaded}"
                            }
                            StartupState.Failed(outcome)
                        }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    // The use case is contractually non-throwing; this branch
                    // is the last-resort safety net for a bug — surface a
                    // retry instead of leaving the splash frozen.
                    logger.error(throwable) { "Startup preload threw unexpectedly" }
                    mutableState.value =
                        StartupState.Failed(
                            outcome = PreloadOutcome(grammarLabelsLoaded = false, taxonomyInvariantsLoaded = false),
                        )
                }
            }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<StartupComponent.Factory>(),
    )
    public fun interface Factory : StartupComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onFinished: () -> Unit,
        ): DefaultStartupComponent
    }
}
