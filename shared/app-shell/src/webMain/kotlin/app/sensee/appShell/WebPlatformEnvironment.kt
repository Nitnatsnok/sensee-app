package app.sensee.appShell

import app.sensee.core.platform.Platform
import app.sensee.core.platform.PlatformContext
import app.sensee.core.platform.PlatformEnvironment

public class WebPlatformEnvironment : PlatformEnvironment {
    override val platform: Platform = webPlatform
    override val context: PlatformContext = PlatformContext()

    // No reliable build-time debug signal in the browser; default to release-safe
    // (no body logging) until a deliberate dev flag is introduced.
    override val isDebug: Boolean = false
}

internal expect val webPlatform: Platform
