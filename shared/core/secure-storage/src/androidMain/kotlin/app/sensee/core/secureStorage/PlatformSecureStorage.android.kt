package app.sensee.core.secureStorage

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.platform.PlatformEnvironment

internal actual fun platformSecureStorage(
    platformEnvironment: PlatformEnvironment,
    appDispatchers: AppDispatchers,
): SecureStorage =
    AndroidSecureStorage(
        context = platformEnvironment.context.applicationContext,
        ioDispatcher = appDispatchers.io,
    )
