package app.sensee.core.mockBackend

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@ContributesTo(AppScope::class)
public interface MockBackendNetworkProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideHttpClientEngine(mockBackend: MockBackend): HttpClientEngine = mockBackend.mockEngine()
}
