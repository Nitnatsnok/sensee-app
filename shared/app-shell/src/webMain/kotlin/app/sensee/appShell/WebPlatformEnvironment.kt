package app.sensee.appShell

import app.sensee.core.platform.Platform
import app.sensee.core.platform.PlatformContext
import app.sensee.core.platform.PlatformEnvironment

public class WebPlatformEnvironment : PlatformEnvironment {
    override val platform: Platform = webPlatform
    override val context: PlatformContext = PlatformContext()
}

internal expect val webPlatform: Platform
