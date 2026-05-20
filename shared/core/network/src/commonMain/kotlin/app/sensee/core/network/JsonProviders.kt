package app.sensee.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
public interface JsonProviders {
    @SingleIn(AppScope::class)
    @Provides
    @OptIn(ExperimentalSerializationApi::class)
    public fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
}
