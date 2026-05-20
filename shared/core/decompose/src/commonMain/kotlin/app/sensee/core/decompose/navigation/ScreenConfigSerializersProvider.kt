package app.sensee.core.decompose.navigation

import kotlinx.serialization.modules.SerializersModule

public interface ScreenConfigSerializersProvider {
    public val serializersModule: SerializersModule
}
