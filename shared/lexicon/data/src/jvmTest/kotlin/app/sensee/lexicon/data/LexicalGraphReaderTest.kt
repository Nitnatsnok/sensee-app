package app.sensee.lexicon.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.database.DefaultDatabaseTransactionRunner
import app.sensee.database.SenseeDatabase
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.lexicon.domain.LexicalEdge
import app.sensee.lexicon.domain.LexicalNode
import app.sensee.lexicon.domain.LexicalRelation
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.WordFamilyMember
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Lexical graph derived at read time over a real in-memory sense store: lemma
 * siblings become navigable sense nodes; word-family / synonym / antonym terms
 * resolve to a stable stored sense sharing their lemma key, else a bare term; and
 * a node reachable two ways is emitted once, as the closest relation.
 */
class LexicalGraphReaderTest {
    private class FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000L)
    }

    private class FakeDbProvider(
        private val db: SenseeDatabase,
    ) : SenseeDatabaseProvider {
        override suspend fun database(): SenseeDatabase = db
    }

    private class SequentialSenseIdFactory : SenseIdFactory {
        private var counter = 0

        override fun mintPersonal(): SenseId = SenseId("p-${++counter}")

        override fun forServiceSource(sourceRef: String): SenseId = SenseId("svc-$sourceRef")
    }

    private fun newGraph(): Pair<DefaultSenseRepository, DefaultLexicalGraphReader> {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY,
                Properties(),
                SenseeDatabase.Schema.synchronous(),
            )
        val provider = FakeDbProvider(SenseeDatabase(driver))
        val repo =
            DefaultSenseRepository(
                databaseProvider = provider,
                transactionRunner = DefaultDatabaseTransactionRunner(provider),
                dispatchers = immediateAppDispatchers(),
                json = Json,
                clock = FixedClock(),
                senseIdFactory = SequentialSenseIdFactory(),
                appDiagnostics = noOpAppDiagnostics(),
            )
        return repo to DefaultLexicalGraphReader(repo)
    }

    private fun sense(
        translation: String,
        surface: String,
        headLemma: String? = null,
        wordFamily: List<String> = emptyList(),
        synonyms: List<String> = emptyList(),
        antonyms: List<String> = emptyList(),
    ): Sense =
        Sense(
            translation = translation,
            surfaceForm = SurfaceForm.parse(surface),
            headLemma = headLemma,
            wordFamily = wordFamily.map { WordFamilyMember(it) },
            synonyms = synonyms,
            antonyms = antonyms,
            contextualApplications = listOf(ContextualApplication(StudiedSentence.parse("I [[$surface]] it."))),
        )

    private suspend fun DefaultSenseRepository.put(sense: Sense): SenseId =
        upsert(sense, SenseStatus.Confirmed, SenseOrigin.Personal).id

    @Test
    fun `edges include lemma siblings as navigable sense nodes`() =
        runTest {
            val (repo, graph) = newGraph()
            val first = repo.put(sense("приходить", "come", headLemma = "come"))
            val second = repo.put(sense("возникать", "come up", headLemma = "come"))

            assertEquals(
                listOf(LexicalEdge(first, LexicalNode.SenseRef(second), LexicalRelation.LemmaSibling, 0)),
                graph.edgesFrom(first).filter { it.relation == LexicalRelation.LemmaSibling },
            )
        }

    @Test
    fun `a word-family term resolves to a stored sense sharing its lemma`() =
        runTest {
            val (repo, graph) = newGraph()
            val decide = repo.put(sense("решать", "decide", headLemma = "decide", wordFamily = listOf("decision")))
            val decision = repo.put(sense("решение", "decision", headLemma = "decision"))

            assertEquals(
                listOf(LexicalNode.SenseRef(decision)),
                graph.edgesFrom(decide).filter { it.relation == LexicalRelation.WordFamily }.map { it.to },
            )
        }

    @Test
    fun `a term resolves to a stable representative when several senses share its lemma`() =
        runTest {
            val (repo, graph) = newGraph()
            val decide = repo.put(sense("решать", "decide", headLemma = "decide", wordFamily = listOf("idea")))
            val ideaA = repo.put(sense("мысль", "idea", headLemma = "idea"))
            repo.put(sense("замысел", "idea concept", headLemma = "idea"))

            // Two senses share lemma "idea"; the edge resolves to the lowest-id one.
            assertEquals(
                listOf(LexicalNode.SenseRef(ideaA)),
                graph.edgesFrom(decide).filter { it.relation == LexicalRelation.WordFamily }.map { it.to },
            )
        }

    @Test
    fun `a node reachable as both a sibling and a word-family term is emitted once`() =
        runTest {
            val (repo, graph) = newGraph()
            val come = repo.put(sense("приходить", "come", headLemma = "come", wordFamily = listOf("come")))
            val comeUp = repo.put(sense("возникать", "come up", headLemma = "come"))

            assertEquals(
                listOf(LexicalEdge(come, LexicalNode.SenseRef(comeUp), LexicalRelation.LemmaSibling, 0)),
                graph.edgesFrom(come),
            )
        }

    @Test
    fun `a blank-padded synonym surfaces as a trimmed term`() =
        runTest {
            val (repo, graph) = newGraph()
            val come = repo.put(sense("приходить", "come", headLemma = "come", synonyms = listOf("  arrive  ")))

            assertEquals(
                listOf(LexicalNode.Term("arrive")),
                graph.edgesFrom(come).filter { it.relation == LexicalRelation.Synonym }.map { it.to },
            )
        }

    @Test
    fun `an antonym with no stored sense surfaces as a bare term`() =
        runTest {
            val (repo, graph) = newGraph()
            val come = repo.put(sense("приходить", "come", headLemma = "come", antonyms = listOf("go")))

            assertEquals(
                listOf(LexicalEdge(come, LexicalNode.Term("go"), LexicalRelation.Antonym, 2)),
                graph.edgesFrom(come).filter { it.relation == LexicalRelation.Antonym },
            )
        }

    @Test
    fun `edges from an unknown sense are empty`() =
        runTest {
            val (_, graph) = newGraph()
            assertEquals(0, graph.edgesFrom(SenseId("missing")).size)
        }
}
