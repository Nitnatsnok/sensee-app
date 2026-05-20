package app.sensee.feature.startup.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext

public interface StartupComponent : AppComponent {
    /**
     * Suspends until one-time app warm-up (the grammar label dictionary) is
     * ready, so the splash covers real work instead of an arbitrary delay.
     * Never throws — a failed warm degrades and still completes.
     */
    public suspend fun awaitReady()

    public fun onFinished()

    public fun interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            onFinished: () -> Unit,
        ): StartupComponent
    }
}
