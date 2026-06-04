package app.sensee.feature.library.data.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.feature.library.data.remote.CatalogMockFixtures
import app.sensee.feature.library.data.remote.DeckDto
import app.sensee.feature.library.data.remote.DeckSummaryDto
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseReadRepository
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.StoredSense
import app.sensee.lexicon.domain.WriteIntent
import app.sensee.lexicon.domain.deriveLemmaKey
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.InMemorySrsStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Catalog projection over the canonical sense store: deck subscription is a flag
 * flip that survives re-sync, Service ingest mirrors senses keyed by source_ref,
 * and the captured deck plus card/lemma reads project from sense_id (so a form
 * card id resolves back to its owning sense).
 */
class CatalogLocalDataSourceTest {
    private val json = Json { ignoreUnknownKeys = true }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    // In-memory canonical store: Service ingest is deterministic by source_ref
    // (so a re-sync reuses the id), Personal mints a sequential id.
    private class InMemorySenseStore :
        SenseReadRepository,
        SenseWriteRepository {
        private val rows = LinkedHashMap<String, StoredSense>()
        private val updates = MutableStateFlow<List<StoredSense>>(emptyList())
        private var personalSeq = 0

        override suspend fun getById(id: SenseId): StoredSense? = rows[id.value]

        override fun observe(): Flow<List<StoredSense>> = updates

        override suspend fun listByStatus(status: SenseStatus): List<StoredSense> =
            rows.values.filter {
                it.status ==
                    status
            }

        override suspend fun listByLemmaKey(lemmaKey: String): List<StoredSense> =
            rows.values.filter {
                it.lemmaKey ==
                    lemmaKey
            }

        override suspend fun upsert(
            sense: Sense,
            status: SenseStatus,
            origin: SenseOrigin,
            sourceRef: String?,
            intent: WriteIntent,
        ): StoredSense {
            val id =
                when (origin) {
                    SenseOrigin.Service -> "svc-${requireNotNull(sourceRef)}"
                    SenseOrigin.Personal -> "p-${++personalSeq}"
                }
            val stored =
                StoredSense(
                    id = SenseId(id),
                    status = status,
                    origin = origin,
                    sourceRef = sourceRef,
                    lemmaKey = deriveLemmaKey(sense),
                    updatedAtEpochMs = 0L,
                    sense = sense,
                )
            rows[id] = stored
            updates.value = rows.values.toList()
            return stored
        }

        override suspend fun confirmAll(senses: List<Sense>): List<StoredSense> =
            senses.map { upsert(it, SenseStatus.Confirmed, SenseOrigin.Personal) }
    }

    private fun newSource(store: InMemorySenseStore = InMemorySenseStore()): CatalogLocalDataSource {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), SenseeDatabase.Schema.synchronous())
        return CatalogLocalDataSource(
            databaseProvider = FakeDbProvider(SenseeDatabase(driver)),
            senseReadRepository = store,
            senseWriteRepository = store,
            srsStorage = InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6()),
            dispatchers = immediateAppDispatchers(),
            appDiagnostics = noOpAppDiagnostics(),
        )
    }

    private fun confirmableSense(
        translation: String,
        surface: String,
        forms: IrregularForms? = null,
    ): Sense =
        Sense(
            translation = translation,
            surfaceForm = SurfaceForm.parse(surface),
            contextualApplications = listOf(ContextualApplication(StudiedSentence.parse("I [[$surface]] it."))),
            irregularForms = forms,
        )

    @Test
    fun `a synced service deck is a passive cache with its advertised card count`() =
        runTest {
            val source = newSource()
            source.upsertDeckSummaries(listOf(DeckSummaryDto("d1", "Title", "Desc", cardCount = 12)))

            val deck = source.selectDecks().single()
            assertEquals(CatalogOrigin.Service, deck.origin, "not subscribed yet")
            assertEquals(12, deck.cardCount, "advertised count survives without local membership")
        }

    @Test
    fun `subscribing flips the deck to owned and a re-sync preserves it`() =
        runTest {
            val source = newSource()
            source.upsertDeckSummaries(listOf(DeckSummaryDto("d1", "Title", "Desc", cardCount = 12)))

            source.setDeckSubscribed("d1", subscribed = true)
            assertEquals(CatalogOrigin.Personal, source.selectDecks().single().origin)

            // A later Service re-sync refreshes metadata but must not clear the flag.
            source.upsertDeckSummaries(listOf(DeckSummaryDto("d1", "Title v2", "Desc", cardCount = 15)))
            val deck = source.selectDecks().single()
            assertEquals(CatalogOrigin.Personal, deck.origin, "subscription survives re-sync")
            assertEquals(15, deck.cardCount)
        }

    @Test
    fun `ingesting a service deck mirrors its senses and projects them as cards`() =
        runTest {
            val store = InMemorySenseStore()
            val source = newSource(store)
            val deck =
                json.decodeFromString<DeckDto>(
                    CatalogMockFixtures().fixtures.getValue("practice/decks/phrasal-verbs-come"),
                )

            source.ingestDeck(deck)

            val projected = source.selectDeckWithCards(deck.id)
            assertNotNull(projected)
            assertTrue(projected.cards.isNotEmpty(), "membership senses project as cards")
            assertTrue(projected.cards.all { it.sense != null }, "each card carries its rich sense")
            assertTrue(
                store.listByStatus(SenseStatus.Confirmed).all { it.origin == SenseOrigin.Service },
                "ingested senses are Service-origin",
            )
        }

    @Test
    fun `the captured deck projects personal confirmed senses keyed by sense_id`() =
        runTest {
            val store = InMemorySenseStore()
            val source = newSource(store)
            val first =
                store.upsert(
                    confirmableSense("наткнуться", "come across"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                )
            val second =
                store.upsert(
                    confirmableSense("предложить", "come up with"),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                )

            val captured = source.capturedDeck()

            assertNotNull(captured)
            assertEquals(
                setOf(first.id.value, second.id.value),
                captured.cards.map { it.id.value }.toSet(),
                "card id IS the sense_id",
            )
        }

    @Test
    fun `a form card id resolves back to its owning sense`() =
        runTest {
            val store = InMemorySenseStore()
            val source = newSource(store)
            val stored =
                store.upsert(
                    confirmableSense("бросать", "throw", forms = IrregularForms("throw", "threw", "thrown")),
                    SenseStatus.Confirmed,
                    SenseOrigin.Personal,
                )

            val formCard = source.capturedDeck()!!.cards.first { it.id.value.contains(":form:") }
            assertTrue(formCard.id.value.startsWith("${stored.id.value}:form:"))

            val resolved = source.selectCard(formCard.id.value)
            assertEquals(formCard.id, resolved?.id, "the form card resolves through its sense_id prefix")
        }
}
