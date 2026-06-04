package app.sensee.lexicon.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.SurfaceToken
import app.sensee.lexicon.domain.SearchPort
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseQuery
import app.sensee.lexicon.domain.SenseReadRepository
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.StoredSense
import app.sensee.lexicon.domain.WriteIntent
import app.sensee.lexicon.domain.deriveLemmaKey
import app.sensee.lexicon.domain.deriveSenseContentKey
import app.sensee.lexicon.domain.requireConfirmable
import app.sensee.lexicon.serialization.SenseDto
import app.sensee.lexicon.serialization.toDomain
import app.sensee.lexicon.serialization.toDto
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import app.sensee.core.database.Sense as SenseRow

/**
 * The single durable sense store (ADR-001/002): one impl behind the split read
 * and write contracts. The [SenseDto] blob is the truth; derived columns
 * (`lemma_key`/`content_key`/`cefr`/`unit_type`) are recomputed on every upsert.
 *
 * [upsert] resolves identity per [WriteIntent] inside one transaction so a
 * resolve-then-write race in the same origin scope cannot fork two ids, and
 * enforces the confirm-gate on `Confirmed` writes. The mutex serializes writers
 * at the app level on top of the SQL transaction.
 */
@SingleIn(AppScope::class)
@Inject
public class DefaultSenseRepository(
    private val databaseProvider: SenseeDatabaseProvider,
    private val dispatchers: AppDispatchers,
    private val json: Json,
    private val clock: Clock,
    private val senseIdFactory: SenseIdFactory,
    appDiagnostics: AppDiagnostics,
) : SenseReadRepository,
    SenseWriteRepository,
    SearchPort {
    private val writeMutex = Mutex()
    private val logger: AppLogger = appDiagnostics.logger.tag("DefaultSenseRepository")

    override suspend fun getById(id: SenseId): StoredSense? =
        databaseProvider
            .database()
            .senseQueries
            .selectById(id.value)
            .awaitAsOneOrNull()
            ?.toStoredSenseOrNull()

    override fun observe(): Flow<List<StoredSense>> =
        flow {
            val database = databaseProvider.database()
            emitAll(
                database.senseQueries
                    .selectAll()
                    .asFlow()
                    .mapToList(dispatchers.io)
                    .map { rows -> rows.mapNotNull { it.toStoredSenseOrNull() } },
            )
        }

    override suspend fun listByStatus(status: SenseStatus): List<StoredSense> =
        databaseProvider
            .database()
            .senseQueries
            .selectByStatus(status.name)
            .awaitAsList()
            .mapNotNull { it.toStoredSenseOrNull() }

    override suspend fun listByLemmaKey(lemmaKey: String): List<StoredSense> =
        databaseProvider
            .database()
            .senseQueries
            .selectByLemmaKey(lemmaKey)
            .awaitAsList()
            .mapNotNull { it.toStoredSenseOrNull() }

    override suspend fun search(query: SenseQuery): List<StoredSense> {
        val term = query.text.trim().lowercase()
        if (term.isEmpty()) return emptyList()
        val queries = databaseProvider.database().senseQueries
        val status = query.status
        val rows =
            if (status == null) {
                queries.selectAll().awaitAsList()
            } else {
                queries.selectByStatus(status.name).awaitAsList()
            }
        return rows
            .asSequence()
            .mapNotNull { it.toStoredSenseOrNull() }
            .mapNotNull { stored -> stored.matchRank(term)?.let { RankedSense(stored, it) } }
            .sortedWith(compareBy<RankedSense> { it.rank }.thenByDescending { it.sense.updatedAtEpochMs })
            .take(query.limit)
            .map { it.sense }
            .toList()
    }

    override suspend fun upsert(
        sense: Sense,
        status: SenseStatus,
        origin: SenseOrigin,
        sourceRef: String?,
        intent: WriteIntent,
    ): StoredSense {
        require(origin != SenseOrigin.Service || sourceRef != null) { SERVICE_SOURCE_REF_REQUIRED }
        if (status == SenseStatus.Confirmed) sense.requireConfirmable()
        return writeMutex.withLock {
            val database = databaseProvider.database()
            database.transactionWithResult {
                val id = resolveId(database, sense, origin, sourceRef, intent)
                writeRow(database, sense, ResolvedWrite(id, status, origin, sourceRef))
            }
        }
    }

    override suspend fun confirmAll(senses: List<Sense>): List<StoredSense> {
        senses.forEach { it.requireConfirmable() }
        return writeMutex.withLock {
            val database = databaseProvider.database()
            database.transactionWithResult {
                senses.map { sense ->
                    val id = resolveId(database, sense, SenseOrigin.Personal, null, WriteIntent.ResolveOrMint)
                    writeRow(database, sense, ResolvedWrite(id, SenseStatus.Confirmed, SenseOrigin.Personal, null))
                }
            }
        }
    }

    /** The resolved identity and row metadata for a single sense write. */
    private class ResolvedWrite(
        val id: SenseId,
        val status: SenseStatus,
        val origin: SenseOrigin,
        val sourceRef: String?,
    )

    /**
     * Write one resolved sense row inside a transaction the caller already owns
     * (with the write lock held), so a single [upsert] and a [confirmAll] batch
     * share the same row-write step without re-entering the lock or opening a
     * nested transaction.
     */
    private suspend fun writeRow(
        database: SenseeDatabase,
        sense: Sense,
        write: ResolvedWrite,
    ): StoredSense {
        val updatedAt = clock.now().toEpochMilliseconds()
        val lemmaKey = deriveLemmaKey(sense)
        database.senseQueries.upsert(
            sense_id = write.id.value,
            status = write.status.name,
            sense_json = json.encodeToString(SenseDto.serializer(), sense.toDto()),
            lemma_key = lemmaKey,
            content_key = deriveSenseContentKey(sense),
            origin = write.origin.name,
            source_ref = write.sourceRef,
            cefr = sense.cefr?.name,
            unit_type = sense.unitType?.id,
            updated_at_epoch_ms = updatedAt,
        )
        return StoredSense(
            id = write.id,
            status = write.status,
            origin = write.origin,
            sourceRef = write.sourceRef,
            lemmaKey = lemmaKey,
            updatedAtEpochMs = updatedAt,
            sense = sense,
        )
    }

    /**
     * Identity for this write. [WriteIntent.UpdateExisting] keeps the given id;
     * [WriteIntent.ForceMint] always mints; [WriteIntent.ResolveOrMint] reuses an
     * existing id on an origin-scoped match and mints on a miss.
     */
    private suspend fun resolveId(
        database: SenseeDatabase,
        sense: Sense,
        origin: SenseOrigin,
        sourceRef: String?,
        intent: WriteIntent,
    ): SenseId =
        when (intent) {
            is WriteIntent.UpdateExisting -> intent.id
            WriteIntent.ForceMint ->
                when (origin) {
                    SenseOrigin.Personal -> senseIdFactory.mintPersonal()
                    SenseOrigin.Service ->
                        senseIdFactory.forServiceSource(requireNotNull(sourceRef) { SERVICE_SOURCE_REF_REQUIRED })
                }
            WriteIntent.ResolveOrMint ->
                when (origin) {
                    SenseOrigin.Personal -> resolveOrMintPersonal(database, sense)
                    SenseOrigin.Service ->
                        resolveOrMintService(database, requireNotNull(sourceRef) { SERVICE_SOURCE_REF_REQUIRED })
                }
        }

    /**
     * Reuse the newest Personal row with the same `content_key` (deterministic
     * tie-break), else mint. A blank-translation draft always mints — its key is
     * not yet full, so two unfinished drafts of one word stay separate authoring
     * seats instead of collapsing; dedup happens on confirm, where the key is full.
     */
    private suspend fun resolveOrMintPersonal(
        database: SenseeDatabase,
        sense: Sense,
    ): SenseId {
        if (sense.translation.isBlank()) return senseIdFactory.mintPersonal()
        return database.senseQueries
            .selectByContentKeyPersonal(deriveSenseContentKey(sense))
            .awaitAsList()
            .firstOrNull()
            ?.let { SenseId(it.sense_id) }
            ?: senseIdFactory.mintPersonal()
    }

    /** Reuse the row already keyed by this `source_ref`, else mint its namespaced id. */
    private suspend fun resolveOrMintService(
        database: SenseeDatabase,
        sourceRef: String,
    ): SenseId =
        database.senseQueries
            .selectBySourceRef(sourceRef)
            .awaitAsOneOrNull()
            ?.let { SenseId(it.sense_id) }
            ?: senseIdFactory.forServiceSource(sourceRef)

    private fun SenseRow.toStoredSenseOrNull(): StoredSense? {
        val parsedStatus = SenseStatus.entries.firstOrNull { it.name == status }
        if (parsedStatus == null) {
            logger.warn { "Unknown SenseStatus '$status' for sense $sense_id; skipping row." }
            return null
        }
        val parsedOrigin = SenseOrigin.entries.firstOrNull { it.name == origin }
        if (parsedOrigin == null) {
            logger.warn { "Unknown SenseOrigin '$origin' for sense $sense_id; skipping row." }
            return null
        }
        val parsedSense =
            try {
                json.decodeFromString(SenseDto.serializer(), sense_json).toDomain()
            } catch (failure: SerializationException) {
                logger.warn(failure) { "Malformed sense_json for sense $sense_id; skipping row." }
                return null
            }
        return StoredSense(
            id = SenseId(sense_id),
            status = parsedStatus,
            origin = parsedOrigin,
            sourceRef = source_ref,
            lemmaKey = lemma_key,
            updatedAtEpochMs = updated_at_epoch_ms,
            sense = parsedSense,
        )
    }

    private companion object {
        const val SERVICE_SOURCE_REF_REQUIRED = "A Service sense requires a source_ref"
    }
}

private enum class SenseMatchRank { EXACT, PREFIX, SUBSTRING }

private class RankedSense(
    val sense: StoredSense,
    val rank: SenseMatchRank,
)

// Lower rank = better match; null = no match. Surface form (literal tokens only,
// not argument-slot placeholder names), translation, and lemma key are matched
// the same way — exact, then prefix, then any substring — so a query ranks
// consistently whether it hits the L2 form, the L1 translation, or the head lemma.
private fun StoredSense.matchRank(term: String): SenseMatchRank? {
    val haystacks =
        listOfNotNull(
            sense.surfaceForm?.searchableText()?.lowercase(),
            sense.translation.lowercase(),
            lemmaKey.lowercase(),
        )
    return when {
        haystacks.any { it == term } -> SenseMatchRank.EXACT
        haystacks.any { it.startsWith(term) } -> SenseMatchRank.PREFIX
        haystacks.any { it.contains(term) } -> SenseMatchRank.SUBSTRING
        else -> null
    }
}

// The lexical text to match against: literal and optional-particle tokens, not
// argument-slot names (`<something>` is a placeholder, not searchable material).
private fun SurfaceForm.searchableText(): String {
    val parts =
        tokens.mapNotNull { token ->
            when (token) {
                is SurfaceToken.Literal -> token.text
                is SurfaceToken.Optional -> token.text
                is SurfaceToken.Slot -> null
            }
        }
    return parts.joinToString(" ")
}
