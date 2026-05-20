package app.sensee.feature.vocabularyEditor.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.database.Lexical_entry
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.vocabularyEditor.domain.EntryId
import app.sensee.feature.vocabularyEditor.domain.EntryStatus
import app.sensee.feature.vocabularyEditor.domain.LexicalEntry
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.feature.vocabularyEditor.domain.VocabularyRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Durable, feature-owned persistence for captured entries (ADR-002): rows in
 * the aggregate `SenseeDatabase`, confirmed meanings serialized as JSON via
 * [MeaningDto] (domain stays free of serialization). Replaces the in-memory
 * PoC so capture survives process death; the rich sense model round-trips
 * through the same neutral parse/resolve the AI boundary uses.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<VocabularyRepository>(),
)
@Inject
public class DurableVocabularyRepository(
    private val databaseProvider: SenseeDatabaseProvider,
    private val dispatchers: AppDispatchers,
    private val json: Json,
    private val clock: Clock,
) : VocabularyRepository {
    private val mutationMutex = Mutex()
    private var sequence = 0

    override suspend fun createDraft(term: String): LexicalEntry =
        mutationMutex.withLock {
            val id = EntryId("entry-${clock.now().toEpochMilliseconds()}-${++sequence}")
            val entry = LexicalEntry(id = id, term = term.trim(), status = EntryStatus.Draft)
            upsert(entry)
            entry
        }

    override suspend fun updateDraftTerm(
        id: EntryId,
        term: String,
    ): LexicalEntry? =
        mutationMutex.withLock {
            databaseProvider.database().transactionWithResult {
                val current = getEntry(id) ?: return@transactionWithResult null
                if (current.status != EntryStatus.Draft) return@transactionWithResult null
                val updated = current.copy(term = term.trim())
                upsert(updated)
                updated
            }
        }

    override suspend fun getEntry(id: EntryId): LexicalEntry? =
        databaseProvider
            .database()
            .lexicalEntryEntityQueries
            .selectEntry(id.value)
            .awaitAsOneOrNull()
            ?.toDomain()

    override suspend fun listEntries(): List<LexicalEntry> =
        databaseProvider
            .database()
            .lexicalEntryEntityQueries
            .selectAllEntries()
            .awaitAsList()
            .map { it.toDomain() }

    override fun observeEntries(): Flow<List<LexicalEntry>> =
        flow {
            val database = databaseProvider.database()
            emitAll(
                database.lexicalEntryEntityQueries
                    .selectAllEntries()
                    .asFlow()
                    .mapToList(dispatchers.io)
                    .map { rows -> rows.map { it.toDomain() } },
            )
        }

    override suspend fun confirmMeanings(
        id: EntryId,
        meanings: List<Meaning>,
    ): LexicalEntry =
        mutationMutex.withLock {
            // Read-modify-write is one transaction: the repository mutex serializes
            // draft updates and confirms; the DB transaction keeps the read/write
            // pair atomic so a concurrent confirm or a confirm racing a draft upsert
            // cannot last-writer-wins meanings away.
            val database = databaseProvider.database()
            database.transactionWithResult {
                val current = getEntry(id) ?: error("Unknown entry $id")
                if (current.status != EntryStatus.Draft || meanings.isEmpty()) {
                    current
                } else {
                    val updated =
                        current.copy(
                            status = EntryStatus.Confirmed,
                            meanings = current.meanings + meanings,
                        )
                    upsert(updated)
                    updated
                }
            }
        }

    override suspend fun deleteEntry(id: EntryId): Unit =
        mutationMutex.withLock {
            databaseProvider
                .database()
                .lexicalEntryEntityQueries
                .deleteEntry(id.value)
            Unit
        }

    private suspend fun upsert(entry: LexicalEntry) {
        databaseProvider
            .database()
            .lexicalEntryEntityQueries
            .upsertEntry(
                id = entry.id.value,
                term = entry.term,
                status = entry.status.name,
                meanings_json = json.encodeToString(entry.meanings.map { it.toDto() }),
                updated_at_epoch_ms = clock.now().toEpochMilliseconds(),
            )
    }

    private fun Lexical_entry.toDomain(): LexicalEntry =
        LexicalEntry(
            id = EntryId(id),
            term = term,
            status = EntryStatus.entries.firstOrNull { it.name == status } ?: EntryStatus.Draft,
            meanings =
                try {
                    json.decodeFromString<List<MeaningDto>>(meanings_json).map { it.toDomain() }
                } catch (_: SerializationException) {
                    emptyList()
                },
        )
}
