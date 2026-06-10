package app.sensee.feature.practice.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.practice.domain.DuePracticeRepository
import app.sensee.srs.core.id.SrsCardId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/**
 * Counts due SRS cards directly over the practice SRS table, so a dashboard gets a
 * due number without the catalog projection. [observeDueCount] re-emits as reviews
 * change the table.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<DuePracticeRepository>(),
)
@Inject
public class DefaultDuePracticeRepository(
    private val databaseProvider: SenseeDatabaseProvider,
    private val dispatchers: AppDispatchers,
) : DuePracticeRepository {
    override suspend fun countDue(now: Instant): Int =
        databaseProvider
            .database()
            .practiceSrsEntityQueries
            .countDue(now.toEpochMilliseconds())
            .awaitAsOne()
            .toInt()

    override fun observeDueCount(now: Instant): Flow<Int> =
        flow {
            val database = databaseProvider.database()
            emitAll(
                database.practiceSrsEntityQueries
                    .countDue(now.toEpochMilliseconds())
                    .asFlow()
                    .mapToOne(dispatchers.io)
                    .map { it.toInt() },
            )
        }

    override suspend fun dueCardIds(
        now: Instant,
        limit: Int,
    ): List<SrsCardId> {
        if (limit <= 0) return emptyList()
        return databaseProvider
            .database()
            .practiceSrsEntityQueries
            .selectDuePracticeSrsCards(
                due_at_epoch_ms = now.toEpochMilliseconds(),
                value_ = limit.toLong(),
            ).awaitAsList()
            .map { SrsCardId(it.card_id) }
    }
}
