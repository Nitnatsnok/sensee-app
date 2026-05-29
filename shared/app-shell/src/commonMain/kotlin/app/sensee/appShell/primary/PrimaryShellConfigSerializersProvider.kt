package app.sensee.appShell.primary

import app.sensee.appShell.root.StartupConfig
import app.sensee.core.decompose.navigation.ScreenConfigSerializersProvider
import app.sensee.core.decompose.navigation.screenConfigSerializersModule
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

@ContributesIntoSet(AppScope::class)
@Inject
public class PrimaryShellConfigSerializersProvider : ScreenConfigSerializersProvider {
    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule: SerializersModule =
        screenConfigSerializersModule {
            // Root stack configs: persisted so a process-death restore
            // returns the user's deep target instead of the splash.
            subclass(StartupConfig::class, serializer<StartupConfig>())
            subclass(
                PrimaryShellConfig::class,
                serializer<PrimaryShellConfig>(),
            )
            subclassesOfSealed<PrimarySectionConfig>()
        }
}
