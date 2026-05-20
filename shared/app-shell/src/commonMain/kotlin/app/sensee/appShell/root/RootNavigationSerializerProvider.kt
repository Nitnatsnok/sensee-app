package app.sensee.appShell.root

import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.ScreenConfigSerializerFactory
import app.sensee.core.decompose.navigation.ScreenConfigSerializersProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.KSerializer

@SingleIn(AppScope::class)
@Inject
public class RootNavigationSerializerProvider(
    providers: Set<ScreenConfigSerializersProvider>,
) {
    public val configSerializer: KSerializer<ScreenConfig> =
        ScreenConfigSerializerFactory(providers).create()
}
