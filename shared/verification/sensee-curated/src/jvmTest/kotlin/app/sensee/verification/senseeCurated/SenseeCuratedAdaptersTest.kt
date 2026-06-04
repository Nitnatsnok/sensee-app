package app.sensee.verification.senseeCurated

import app.sensee.verification.core.contract.LexicalVerificationQuery
import app.sensee.verification.core.contract.VerifierAvailability
import app.sensee.verification.core.hierarchy.LemmaId
import app.sensee.verification.senseeCurated.remote.SenseeCuratedMockFixtures
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SenseeCuratedAdaptersTest {
    private val fixtures = SenseeCuratedMockFixtures().fixtures

    @Test
    fun `frequency resolves a curated lemma with a band`() =
        runTest {
            val result = SenseeFrequencyProvider(catalogs()).frequency(query("come"))

            assertEquals(VerifierAvailability.Available, result.availability)
            assertNotNull(result.score)
        }

    @Test
    fun `frequency degrades on a lemma the catalog does not cover`() =
        runTest {
            val result = SenseeFrequencyProvider(catalogs()).frequency(query("zxqwvbn"))

            assertTrue(result.availability is VerifierAvailability.Degraded)
        }

    @Test
    fun `frequency is unavailable when the backend cannot be reached`() =
        runTest {
            val result = SenseeFrequencyProvider(catalogs(serve = false)).frequency(query("come"))

            assertTrue(result.availability is VerifierAvailability.Unavailable)
        }

    @Test
    fun `a non-English query is unsupported`() =
        runTest {
            val result = SenseeFrequencyProvider(catalogs()).frequency(query("come", language = "fr"))

            assertTrue(result.availability is VerifierAvailability.Unavailable)
        }

    @Test
    fun `cefr resolves a curated level`() =
        runTest {
            val result = SenseeCefrLevelProvider(catalogs()).cefr(query("come"))

            assertEquals(VerifierAvailability.Available, result.availability)
            assertNotNull(result.level)
        }

    @Test
    fun `senses returns the curated inventory as extra dictionary senses`() =
        runTest {
            val result = SenseeSenseInventoryProvider(catalogs()).senses(query("come"))

            assertEquals(VerifierAvailability.Available, result.availability)
            assertTrue(result.mapping.extraDictionarySenses.size >= 2)
            assertTrue(result.mapping.extraDictionarySenses.all { it.shortLabel?.isNotBlank() == true })
        }

    @Test
    fun `family resolveUnit finds a phrasal verb by display form and keeps its head lemma`() =
        runTest {
            val result = SenseeLexicalFamilyProvider(catalogs()).resolveUnit(query("come across"))

            assertEquals(VerifierAvailability.Available, result.availability)
            assertEquals("phrasal_verb", result.unit?.entryType?.id)
            assertEquals(LemmaId.of("en", "come"), result.unit?.headLemma)
        }

    @Test
    fun `family lists siblings for a head lemma`() =
        runTest {
            val result = SenseeLexicalFamilyProvider(catalogs()).family(LemmaId.of("en", "come"), cap = 50)

            assertEquals(VerifierAvailability.Available, result.availability)
            assertTrue(result.units.size >= 2)
        }

    @Test
    fun `family caps siblings and flags truncation`() =
        runTest {
            val result = SenseeLexicalFamilyProvider(catalogs()).family(LemmaId.of("en", "come"), cap = 1)

            assertEquals(1, result.units.size)
            assertTrue(result.truncated)
        }

    private fun catalogs(serve: Boolean = true): SenseeCuratedCatalogs {
        val engine =
            MockEngine { request ->
                val body = if (serve) fixtures[request.url.encodedPath.trimStart('/')] else null
                if (body == null) {
                    respond("""{"error":"not_found"}""", HttpStatusCode.NotFound, JSON_HEADERS)
                } else {
                    respond(body, HttpStatusCode.OK, JSON_HEADERS)
                }
            }
        return SenseeCuratedCatalogs(
            HttpClient(engine) {
                expectSuccess = true
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
        )
    }

    private fun query(
        text: String,
        language: String = "en",
    ): LexicalVerificationQuery = LexicalVerificationQuery(text = text, studyLanguageTag = language)

    private companion object {
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
