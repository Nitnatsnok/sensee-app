package app.sensee.feature.practice.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsScope
import app.sensee.srs.core.log.SrsReviewLog
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.engine.storage.SrsStorage
import app.sensee.srs.fsrs.FsrsAlgorithmState
import app.sensee.srs.fsrs.FsrsParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Instant

@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<SrsStorage<FsrsParameters>>(),
)
@Inject
public class PracticeSrsStorage(
    private val databaseProvider: SenseeDatabaseProvider,
) : SrsStorage<FsrsParameters> {
    private suspend fun database(): SenseeDatabase = databaseProvider.database()

    override suspend fun getCard(cardId: SrsCardId): SrsCardSnapshot? =
        database()
            .practiceSrsEntityQueries
            .selectPracticeSrsCardById(cardId.value)
            .awaitAsOneOrNull()
            ?.toSrsCardSnapshot()

    override suspend fun getCards(cardIds: Collection<SrsCardId>): Map<SrsCardId, SrsCardSnapshot> {
        val ids = cardIds.map { it.value }.distinct()
        if (ids.isEmpty()) return emptyMap()
        return database()
            .practiceSrsEntityQueries
            .selectPracticeSrsCardsByIds(ids)
            .awaitAsList()
            .associate { row -> SrsCardId(row.card_id) to row.toSrsCardSnapshot() }
    }

    override suspend fun saveCard(card: SrsCardSnapshot) {
        database().saveSrsCard(card)
    }

    override suspend fun saveCardIfAbsent(card: SrsCardSnapshot): SrsCardSnapshot {
        val database = database()
        database.insertSrsCardIfAbsent(card)
        return database
            .practiceSrsEntityQueries
            .selectPracticeSrsCardById(card.id.value)
            .awaitAsOneOrNull()
            ?.toSrsCardSnapshot()
            ?: card
    }

    override suspend fun getDueCards(
        now: Instant,
        limit: Int,
    ): List<SrsCardSnapshot> {
        if (limit <= 0) return emptyList()
        return database()
            .practiceSrsEntityQueries
            .selectDuePracticeSrsCards(
                due_at_epoch_ms = now.toEpochMilliseconds(),
                value_ = limit.toLong(),
            ).awaitAsList()
            .map { it.toSrsCardSnapshot() }
    }

    override suspend fun appendReviewLog(log: SrsReviewLog) {
        database().practiceSrsEntityQueries.insertPracticeSrsReviewLog(
            id = log.id.value,
            card_id = log.cardId.value,
            rating = log.rating.name,
            reviewed_at_epoch_ms = log.reviewedAt.toEpochMilliseconds(),
        )
    }

    override suspend fun getReviewLogs(
        cardId: SrsCardId,
        limit: Int?,
    ): List<SrsReviewLog> = emptyList()

    override suspend fun getActiveParameters(scope: SrsScope): FsrsParameters = FsrsParameters.defaultV6()

    override suspend fun saveParameters(
        scope: SrsScope,
        parameters: FsrsParameters,
    ): Unit = Unit

    // SrsEngine.submitReview wraps getCard -> schedule -> saveCard + appendReviewLog in this
    // call expecting atomicity, so the block runs inside a real SQLDelight
    // (async/suspending) transaction — partial failure between the SRS card write and the
    // review-log insert is unacceptable. Nested storage calls go through the same singleton
    // SenseeDatabase and enlist in this enclosing transaction.
    override suspend fun <T> transaction(block: suspend () -> T): T = database().transactionWithResult { block() }
}

internal suspend fun SenseeDatabase.saveSrsCard(card: SrsCardSnapshot) {
    val fsrsState = card.algorithmState as? FsrsAlgorithmState
    practiceSrsEntityQueries.upsertPracticeSrsCard(
        card_id = card.id.value,
        state = card.state.name,
        due_at_epoch_ms = card.dueAt?.toEpochMilliseconds(),
        last_reviewed_at_epoch_ms = card.lastReviewedAt?.toEpochMilliseconds(),
        scheduled_interval_ms = card.scheduledInterval?.inWholeMilliseconds,
        review_count = card.reviewCount.toLong(),
        lapse_count = card.lapseCount.toLong(),
        step_index = card.stepIndex?.toLong(),
        algorithm_name = card.algorithm?.name,
        algorithm_version = card.algorithm?.version,
        fsrs_difficulty = fsrsState?.difficulty,
        fsrs_stability = fsrsState?.stability,
        parameters_id = card.parametersId?.value,
    )
}

private suspend fun SenseeDatabase.insertSrsCardIfAbsent(card: SrsCardSnapshot) {
    val fsrsState = card.algorithmState as? FsrsAlgorithmState
    practiceSrsEntityQueries.insertPracticeSrsCardIfAbsent(
        card_id = card.id.value,
        state = card.state.name,
        due_at_epoch_ms = card.dueAt?.toEpochMilliseconds(),
        last_reviewed_at_epoch_ms = card.lastReviewedAt?.toEpochMilliseconds(),
        scheduled_interval_ms = card.scheduledInterval?.inWholeMilliseconds,
        review_count = card.reviewCount.toLong(),
        lapse_count = card.lapseCount.toLong(),
        step_index = card.stepIndex?.toLong(),
        algorithm_name = card.algorithm?.name,
        algorithm_version = card.algorithm?.version,
        fsrs_difficulty = fsrsState?.difficulty,
        fsrs_stability = fsrsState?.stability,
        parameters_id = card.parametersId?.value,
    )
}
