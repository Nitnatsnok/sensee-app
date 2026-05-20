package app.sensee.core.secureStorage

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.platform.PlatformEnvironment

/**
 * Per-target factory for the platform-native [SecureStorage]. Kept as an
 * `expect fun` (not an actual class) so each platform picks the dependencies
 * it needs out of [PlatformEnvironment] without leaking a Context/NSBundle
 * type into common.
 */
internal expect fun platformSecureStorage(
    platformEnvironment: PlatformEnvironment,
    appDispatchers: AppDispatchers,
): SecureStorage
