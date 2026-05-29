package app.sensee.feature.library.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfigSerializersProvider
import app.sensee.core.decompose.navigation.screenConfigSerializersModule
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule

@ContributesIntoSet(AppScope::class)
@Inject
public class LibraryConfigSerializersProvider : ScreenConfigSerializersProvider {
    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule: SerializersModule =
        screenConfigSerializersModule { subclassesOfSealed<LibraryConfig>() }
}
