package app.sensee.core.secureStorage

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.platform.PlatformEnvironment

private const val DEFAULT_DESKTOP_SERVICE: String = "app.sensee"

internal actual fun platformSecureStorage(
    platformEnvironment: PlatformEnvironment,
    appDispatchers: AppDispatchers,
): SecureStorage =
    DesktopSecureStorage(
        service = DEFAULT_DESKTOP_SERVICE,
        ioDispatcher = appDispatchers.io,
    )
