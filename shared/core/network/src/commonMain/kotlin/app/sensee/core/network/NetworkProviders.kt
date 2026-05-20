package app.sensee.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
public interface NetworkProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideNetworkConfig(): NetworkConfig =
        NetworkConfig(
            baseUrl = "https://mock.sensee.local/",
        )

    @SingleIn(AppScope::class)
    @Provides
    public fun provideNetworkHeadersProvider(): NetworkHeadersProvider = EmptyNetworkHeadersProvider

    @SingleIn(AppScope::class)
    @Provides
    public fun provideNetworkLogger(): NetworkLogger = NoOpNetworkLogger

    @SingleIn(AppScope::class)
    @Provides
    public fun provideHttpClient(
        engine: HttpClientEngine,
        config: NetworkConfig,
        headersProvider: NetworkHeadersProvider,
        logger: NetworkLogger,
        json: Json,
    ): HttpClient =
        NetworkHttpClientFactory.create(
            engine = engine,
            config = config,
            json = json,
            headersProvider = headersProvider,
            logger = logger,
        )
}
