package app.sensee.core.decompose.navigation

import com.arkivanov.essenty.statekeeper.ExperimentalStateKeeperApi
import com.arkivanov.essenty.statekeeper.polymorphicSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule

public class ScreenConfigSerializerFactory(
    private val providers: Set<ScreenConfigSerializersProvider>,
) {
    @OptIn(
        ExperimentalSerializationApi::class,
        ExperimentalStateKeeperApi::class,
    )
    public fun create(): KSerializer<ScreenConfig> {
        val module =
            SerializersModule {
                providers.forEach { provider ->
                    include(provider.serializersModule)
                }
            }

        return polymorphicSerializer(module)
    }
}
