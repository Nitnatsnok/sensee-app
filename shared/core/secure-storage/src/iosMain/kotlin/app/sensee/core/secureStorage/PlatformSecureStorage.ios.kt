package app.sensee.core.secureStorage

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.platform.PlatformEnvironment

private const val DEFAULT_KEYCHAIN_SERVICE: String = "app.sensee.secure_storage"

internal actual fun platformSecureStorage(
    platformEnvironment: PlatformEnvironment,
    appDispatchers: AppDispatchers,
): SecureStorage =
    IosKeychainSecureStorage(
        service = DEFAULT_KEYCHAIN_SERVICE,
        ioDispatcher = appDispatchers.io,
    )
