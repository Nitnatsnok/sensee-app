@file:Suppress("MatchingDeclarationName")

package app.sensee.core.testKit

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.NoOpCrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.diagnostics.DefaultAppDiagnostics
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * Shared harness for `BaseLogic`-style tests: a synchronous main dispatcher and
 * no-op diagnostics. Lives in `core` because every feature's logic test needs
 * the same wiring — see the per-area test-kit convention (`srs`/`tts`).
 */
public class ImmediateMainDispatcher : MainCoroutineDispatcher() {
    override val immediate: MainCoroutineDispatcher = this

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        block.run()
    }
}

/** All dispatchers run the block synchronously on the calling thread. */
public fun immediateAppDispatchers(dispatcher: MainCoroutineDispatcher = ImmediateMainDispatcher()): AppDispatchers =
    AppDispatchers(
        main = dispatcher,
        default = dispatcher,
        io = dispatcher,
    )

public fun noOpAppDiagnostics(): AppDiagnostics =
    DefaultAppDiagnostics(
        logger = DefaultAppLoggerFactory().tagged("Test"),
        crashReporter = NoOpCrashReporter,
        analyticsTracker = NoOpAnalyticsTracker,
    )
