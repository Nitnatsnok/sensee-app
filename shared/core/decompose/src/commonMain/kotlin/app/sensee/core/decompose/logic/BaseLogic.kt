package app.sensee.core.decompose.logic

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.crash.CrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

public abstract class BaseLogic(
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : Logic {
    protected val logicScope: CoroutineScope =
        CoroutineScope(appDispatchers.main.immediate + SupervisorJob())

    protected val logger: AppLogger =
        appDiagnostics.logger.tag(this::class.simpleName ?: "Logic")

    protected val crashReporter: CrashReporter =
        appDiagnostics.crashReporter

    override fun onDestroy() {
        logicScope.cancel()
    }

    /**
     * Like [runCatching] but cooperative with structured concurrency: a [CancellationException]
     * (e.g. [logicScope] cancelled because the component was destroyed mid-load) is rethrown so
     * the coroutine unwinds normally, instead of being captured into a spurious error [Result]
     * and surfaced to the UI as a load failure.
     */
    protected inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
}
