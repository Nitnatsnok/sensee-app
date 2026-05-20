package app.sensee.core.platform

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
public interface PlatformContextProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun providePlatformContext(environment: PlatformEnvironment): PlatformContext = environment.context
}
