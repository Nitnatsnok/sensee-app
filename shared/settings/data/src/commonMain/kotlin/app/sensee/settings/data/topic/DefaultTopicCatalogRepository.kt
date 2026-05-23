package app.sensee.settings.data.topic

import app.sensee.settings.domain.LearningTopic
import app.sensee.settings.domain.TopicCatalogRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The catalog is small and effectively static, so the first successful fetch is
 * cached for the app lifetime. A failed fetch is not cached — the next call
 * retries.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<TopicCatalogRepository>(),
)
@Inject
public class DefaultTopicCatalogRepository(
    private val remoteDataSource: TopicCatalogRemoteDataSource,
) : TopicCatalogRepository {
    private val mutex = Mutex()
    private var cached: List<LearningTopic>? = null

    override suspend fun topics(): List<LearningTopic> =
        mutex.withLock {
            cached ?: fetch().also { cached = it }
        }

    private suspend fun fetch(): List<LearningTopic> =
        remoteDataSource.listTopics().topics.map {
            LearningTopic(
                id = it.id,
                displayName = it.displayName,
                promptKeyword = it.promptKeyword,
            )
        }
}
