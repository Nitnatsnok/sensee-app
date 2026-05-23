package app.sensee.settings.data.topic

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

@SingleIn(AppScope::class)
@Inject
public class TopicCatalogRemoteDataSource(
    private val httpClient: HttpClient,
) {
    public suspend fun listTopics(): TopicCatalogDto = httpClient.get("learning/topics").body()
}
