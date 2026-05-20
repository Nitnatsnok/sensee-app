package app.sensee.appShell

import android.content.Context
import app.sensee.core.platform.Platform
import app.sensee.core.platform.PlatformContext
import app.sensee.core.platform.PlatformEnvironment

public class AndroidPlatformEnvironment(
    applicationContext: Context,
) : PlatformEnvironment {
    override val platform: Platform = Platform.Android
    override val context: PlatformContext =
        PlatformContext(
            applicationContext = applicationContext,
        )
}
