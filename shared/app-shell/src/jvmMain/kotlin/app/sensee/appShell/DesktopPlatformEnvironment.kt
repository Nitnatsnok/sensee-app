package app.sensee.appShell

import app.sensee.core.platform.Platform
import app.sensee.core.platform.PlatformContext
import app.sensee.core.platform.PlatformEnvironment

public class DesktopPlatformEnvironment : PlatformEnvironment {
    override val platform: Platform = Platform.Desktop
    override val context: PlatformContext = PlatformContext()
}
