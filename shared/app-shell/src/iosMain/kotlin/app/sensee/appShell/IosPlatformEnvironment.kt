package app.sensee.appShell

import app.sensee.core.platform.Platform
import app.sensee.core.platform.PlatformContext
import app.sensee.core.platform.PlatformEnvironment

public class IosPlatformEnvironment : PlatformEnvironment {
    override val platform: Platform = Platform.Ios
    override val context: PlatformContext = PlatformContext()
}
