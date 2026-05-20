package app.sensee.core.platform

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock

@ContributesTo(AppScope::class)
public interface PlatformClockProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideClock(): Clock = Clock.System
}
