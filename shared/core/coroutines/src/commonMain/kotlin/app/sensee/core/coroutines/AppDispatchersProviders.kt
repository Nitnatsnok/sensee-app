package app.sensee.core.coroutines

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
public interface AppDispatchersProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideAppDispatchers(): AppDispatchers =
        AppDispatchers(
            main = Dispatchers.Main,
            default = Dispatchers.Default,
            io = appIoDispatcher,
        )
}
