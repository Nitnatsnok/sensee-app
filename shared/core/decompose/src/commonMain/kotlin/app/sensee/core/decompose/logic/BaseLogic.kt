package app.sensee.core.decompose.logic

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.crash.CrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
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
}
