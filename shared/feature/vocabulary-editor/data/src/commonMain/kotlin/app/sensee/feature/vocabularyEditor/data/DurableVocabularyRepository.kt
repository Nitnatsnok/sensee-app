package app.sensee.feature.vocabularyEditor.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.database.Lexical_entry
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.logging.AppLogger
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.vocabularyEditor.domain.VocabularyRepository
import app.sensee.lexicon.domain.EntryId
import app.sensee.lexicon.domain.EntryStatus
import app.sensee.lexicon.domain.LexicalEntry
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.deriveSenseContentKey
import app.sensee.lexicon.serialization.SenseDto
import app.sensee.lexicon.serialization.toDomain
import app.sensee.lexicon.serialization.toDto
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
 * the aggregate `SenseeDatabase`, confirmed senses serialized as JSON via
 * [SenseDto] (domain stays free of serialization). Replaces the in-memory
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
    appDiagnostics: AppDiagnostics,
) : VocabularyRepository {
    private val mutationMutex = Mutex()
    private val logger: AppLogger = appDiagnostics.logger.tag("DurableVocabularyRepository")
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

    override suspend fun confirmSenses(
        id: EntryId,
        senses: List<Sense>,
    ): LexicalEntry =
        mutationMutex.withLock {
            // Read-modify-write is one transaction: the repository mutex serializes
            // draft updates and confirms; the DB transaction keeps the read/write
            // pair atomic so a concurrent confirm or a confirm racing a draft upsert
            // cannot last-writer-wins senses away.
            val database = databaseProvider.database()
            database.transactionWithResult {
                val current = getEntry(id) ?: error("Unknown entry $id")
                if (current.status != EntryStatus.Draft || senses.isEmpty()) {
                    return@transactionWithResult current
                }
                // Cross-entry dedup: a re-capture of "come across" (a new draft)
                // must roll into the existing Confirmed entry instead of forking a
                // second row. Otherwise the SRS state on the existing senses
                // would orphan and the family page would split into two stubs.
                val termKey = current.term.trim().lowercase()
                val existing = findConfirmedByTerm(termKey, except = current.id)
                if (existing != null) {
                    val merged = existing.copy(senses = mergeSenses(existing.senses, senses))
                    upsert(merged)
                    // Drop the empty draft row that prompted the merge — its
                    // identity has already served its purpose as a capture seat.
                    database.lexicalEntryEntityQueries.deleteEntry(current.id.value)
                    return@transactionWithResult merged
                }
                val updated =
                    current.copy(
                        status = EntryStatus.Confirmed,
                        // First-confirm goes through the same content-key dedup
                        // primitive as the merge/edit paths (B1). Otherwise two
                        // AI candidates that collapse to the same content key
                        // would both persist on the first confirm and only get
                        // deduped on a later re-capture — forking SRS state.
                        senses = mergeSenses(current.senses, senses),
                    )
                upsert(updated)
                updated
            }
        }

    override suspend fun updateConfirmedEntry(
        id: EntryId,
        term: String,
        senses: List<Sense>,
    ): LexicalEntry? =
        mutationMutex.withLock {
            val database = databaseProvider.database()
            database.transactionWithResult {
                val current = getEntry(id) ?: return@transactionWithResult null
                if (current.status != EntryStatus.Confirmed || senses.isEmpty()) {
                    return@transactionWithResult null
                }
                val trimmedTerm = term.trim()
                val existing = findConfirmedByTerm(trimmedTerm.lowercase(), except = current.id)
                if (existing != null) {
                    val merged = existing.copy(senses = mergeSenses(existing.senses, senses))
                    upsert(merged)
                    database.lexicalEntryEntityQueries.deleteEntry(current.id.value)
                    return@transactionWithResult merged
                }
                val updated =
                    current.copy(
                        term = trimmedTerm,
                        senses = mergeSenses(existing = emptyList(), incoming = senses),
                    )
                upsert(updated)
                updated
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

    private suspend fun findConfirmedByTerm(
        normalizedTerm: String,
        except: EntryId,
    ): LexicalEntry? =
        databaseProvider
            .database()
            .lexicalEntryEntityQueries
            .selectAllEntries()
            .awaitAsList()
            .asSequence()
            .map { it.toDomain() }
            .firstOrNull { candidate ->
                candidate.id != except &&
                    candidate.status == EntryStatus.Confirmed &&
                    candidate.term.trim().lowercase() == normalizedTerm
            }

    private fun mergeSenses(
        existing: List<Sense>,
        incoming: List<Sense>,
    ): List<Sense> {
        val existingKeys = existing.mapTo(mutableSetOf(), ::deriveSenseContentKey)
        // Same identity → existing wins (keeps SRS state and any user edits); a
        // new key is appended in the order it arrived.
        val newSenses = incoming.filter { existingKeys.add(deriveSenseContentKey(it)) }
        return existing + newSenses
    }

    private suspend fun upsert(entry: LexicalEntry) {
        databaseProvider
            .database()
            .lexicalEntryEntityQueries
            .upsertEntry(
                id = entry.id.value,
                term = entry.term,
                status = entry.status.name,
                senses_json = json.encodeToString(entry.senses.map { it.toDto() }),
                updated_at_epoch_ms = clock.now().toEpochMilliseconds(),
            )
    }

    private fun Lexical_entry.toDomain(): LexicalEntry {
        // Both fallbacks below recover *visible* behavior (the row keeps loading)
        // but represent a data-integrity event the developer must see — silent
        // demotion of a Confirmed entry to an empty Draft would otherwise look
        // like the user lost their work. We log via the project's diagnostics
        // channel and surface the entry id so a follow-up read can correlate.
        val resolvedStatus =
            EntryStatus.entries.firstOrNull { it.name == status } ?: run {
                logger.warn {
                    "Unknown EntryStatus '$status' for entry $id; treating as Draft. " +
                        "Confirmed senses (if any) will not be re-promoted on read."
                }
                EntryStatus.Draft
            }
        val resolvedSenses =
            try {
                json.decodeFromString<List<SenseDto>>(senses_json).map { it.toDomain() }
            } catch (failure: SerializationException) {
                logger.warn(failure) {
                    "Malformed senses_json for entry $id; dropping senses. " +
                        "A re-confirm or re-capture replaces the row in place."
                }
                emptyList()
            }
        return LexicalEntry(
            id = EntryId(id),
            term = term,
            status = resolvedStatus,
            senses = resolvedSenses,
        )
    }
}
