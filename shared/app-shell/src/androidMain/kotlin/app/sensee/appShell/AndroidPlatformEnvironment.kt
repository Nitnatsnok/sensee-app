package app.sensee.appShell

import android.content.Context
import android.content.pm.ApplicationInfo
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
    override val isDebug: Boolean =
        (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
