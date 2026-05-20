package app.sensee.core.secureStorage

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.platform.PlatformEnvironment
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
public interface SecureStorageProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideSecureStorage(
        platformEnvironment: PlatformEnvironment,
        appDispatchers: AppDispatchers,
    ): SecureStorage = platformSecureStorage(platformEnvironment, appDispatchers)
}
