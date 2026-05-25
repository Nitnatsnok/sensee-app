package app.sensee.feature.startup.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.feature.startup.domain.PreloadOutcome
import kotlinx.coroutines.flow.StateFlow

/**
 * Splash host. Owns the cold-start handshake: kick off the runtime-dictionary
 * preload, hold the splash until it returns, then either hand off to the
 * primary shell (success) or surface a retry to the user (failure). UI
 * collects [state] and renders the matching screen.
 */
public interface StartupComponent : AppComponent {
    public val state: StateFlow<StartupState>

    /**
     * Re-run the preload after a failure. No-op unless [state] is
     * [StartupState.Failed]. Provider caches do not memoize failures, so the
     * retry actually re-hits the source.
     */
    public fun retry()

    /** Invoked by the UI host once it has observed [StartupState.Loaded]. */
    public fun onFinished()

    public fun interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            onFinished: () -> Unit,
        ): StartupComponent
    }
}

public sealed interface StartupState {
    public data object Loading : StartupState

    public data object Loaded : StartupState

    public data class Failed(
        val outcome: PreloadOutcome,
    ) : StartupState
}
