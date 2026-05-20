package app.sensee.feature.library.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.ScreenConfigSerializersProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@ContributesIntoSet(AppScope::class)
@Inject
public class LibraryConfigSerializersProvider : ScreenConfigSerializersProvider {
    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule: SerializersModule =
        SerializersModule {
            polymorphic(ScreenConfig::class) {
                subclassesOfSealed<LibraryConfig>()
            }
        }
}
