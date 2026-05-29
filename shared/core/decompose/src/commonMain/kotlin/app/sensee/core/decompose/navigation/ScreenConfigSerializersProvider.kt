package app.sensee.core.decompose.navigation

import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

public interface ScreenConfigSerializersProvider {
    public val serializersModule: SerializersModule
}

/**
 * Builds the polymorphic [SerializersModule] for the [ScreenConfig] hierarchy.
 * A [ScreenConfigSerializersProvider] supplies only its own [register] block
 * (typically `subclassesOfSealed<FeatureConfig>()`); the base-class plumbing
 * lives here so the providers don't repeat it.
 */
public fun screenConfigSerializersModule(
    register: PolymorphicModuleBuilder<ScreenConfig>.() -> Unit,
): SerializersModule =
    SerializersModule {
        polymorphic(ScreenConfig::class) {
            register()
        }
    }
