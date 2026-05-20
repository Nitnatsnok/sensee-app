package app.sensee.core.coroutines

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@ContributesTo(AppScope::class)
public interface AppCoroutineScopesProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideAppCoroutineScopes(appDispatchers: AppDispatchers): AppCoroutineScopes =
        AppCoroutineScopes(
            applicationScope = CoroutineScope(appDispatchers.default + SupervisorJob()),
        )
}
