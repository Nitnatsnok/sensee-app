package app.sensee.ai.curatedEnrichment

import app.sensee.ai.core.AiEnrichmentExtension
import app.sensee.ai.core.CefrEnrichmentExtension
import app.sensee.ai.core.EnrichmentAvailability
import app.sensee.ai.core.EnrichmentRequest
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
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CuratedAiEnrichmentClientTest {
    @Test
    fun `a covered lemma returns its curated suggestions`() =
        runTest {
            val client = curatedClient { path -> if (path == "enrichment/come-across") COME_ACROSS else null }

            val result = client.enrich(EnrichmentRequest(term = "come across"))

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertEquals(2, result.suggestions.size)
        }

    @Test
    fun `a slugged lemma is encoded as one path segment`() =
        runTest {
            val client = curatedClient { path -> if (path == "enrichment/a%2Fb%3F-%23") ONE_ITEM else null }

            val result = client.enrich(EnrichmentRequest(term = "A/B? #"))

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertEquals(1, result.suggestions.size)
        }

    @Test
    fun `a 404 is a curated miss and returns an empty available result`() =
        runTest {
            val client = curatedClient { null }

            val result = client.enrich(EnrichmentRequest(term = "unknown"))

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertTrue(result.suggestions.isEmpty())
        }

    @Test
    fun `a server error degrades instead of masking as a miss`() =
        runTest {
            val engine = MockEngine { respond("boom", HttpStatusCode.InternalServerError) }
            val client = CuratedAiEnrichmentClient(HttpClient(engine) { expectSuccess = true }, emptySet())

            val result = client.enrich(EnrichmentRequest(term = "come across"))

            assertTrue(result.availability is EnrichmentAvailability.Degraded)
        }

    @Test
    fun `an unsupported schema version degrades without using items`() =
        runTest {
            val body = """{"version":99,"items":[{"translation":"old shape"}]}"""
            val client = curatedClient { path -> if (path == "enrichment/x") body else null }

            val result = client.enrich(EnrichmentRequest(term = "x"))

            assertTrue(result.availability is EnrichmentAvailability.Degraded)
            assertTrue(result.suggestions.isEmpty())
        }

    @Test
    fun `a non integer schema version degrades without using items`() =
        runTest {
            val body = """{"version":"1","items":[{"translation":"quoted version"}]}"""
            val client = curatedClient { path -> if (path == "enrichment/x") body else null }

            val result = client.enrich(EnrichmentRequest(term = "x"))

            assertTrue(result.availability is EnrichmentAvailability.Degraded)
            assertTrue(result.suggestions.isEmpty())
        }

    @Test
    fun `a covered response without an items array degrades instead of masking as a miss`() =
        runTest {
            val body = """{"version":1,"entries":[{"translation":"hidden"}]}"""
            val client = curatedClient { path -> if (path == "enrichment/x") body else null }

            val result = client.enrich(EnrichmentRequest(term = "x"))

            assertTrue(result.availability is EnrichmentAvailability.Degraded)
            assertTrue(result.suggestions.isEmpty())
        }

    @Test
    fun `a covered response with an empty items array degrades instead of masking as a miss`() =
        runTest {
            val body = """{"version":1,"items":[]}"""
            val client = curatedClient { path -> if (path == "enrichment/x") body else null }

            val result = client.enrich(EnrichmentRequest(term = "x"))

            assertTrue(result.availability is EnrichmentAvailability.Degraded)
            assertTrue(result.suggestions.isEmpty())
        }

    @Test
    fun `a malformed item degrades but keeps the valid suggestions`() =
        runTest {
            val body =
                """
                {"version": 1,"items":[
                  {"translation":"ok","surface_form":"x","unit_type":"verb"},
                  {"components":[{"text":"x"}]}
                ]}
                """.trimIndent()
            val client = curatedClient { path -> if (path == "enrichment/x") body else null }

            val result = client.enrich(EnrichmentRequest(term = "x"))

            assertTrue(result.availability is EnrichmentAvailability.Degraded)
            assertEquals(1, result.suggestions.size)
        }

    @Test
    fun `a registered extension surfaces its owned field on the suggestion`() =
        runTest {
            val body = """{"version":1,"items":[{"translation":"наткнуться","cefr":"B2"}]}"""
            val client =
                curatedClient(extensions = setOf(CefrEnrichmentExtension)) { path ->
                    if (path == "enrichment/x") body else null
                }

            val result = client.enrich(EnrichmentRequest(term = "x"))

            assertEquals(
                "B2",
                result.suggestions
                    .single()
                    .extensions["cefr"]
                    ?.jsonPrimitive
                    ?.content,
            )
        }

    private fun curatedClient(
        extensions: Set<AiEnrichmentExtension> = emptySet(),
        respondFor: (String) -> String?,
    ): CuratedAiEnrichmentClient {
        val engine =
            MockEngine { request ->
                when (val body = respondFor(request.url.encodedPath.trimStart('/'))) {
                    null -> respond("""{"error":"not_found"}""", HttpStatusCode.NotFound, JSON_HEADERS)
                    else -> respond(body, HttpStatusCode.OK, JSON_HEADERS)
                }
            }
        return CuratedAiEnrichmentClient(
            HttpClient(engine) {
                expectSuccess = true
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            extensions,
        )
    }

    private companion object {
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        const val COME_ACROSS =
            """{"version": 1,"items":[{"translation":"наткнуться"},{"translation":"производить впечатление"}]}"""
        const val ONE_ITEM = """{"version": 1,"items":[{"translation":"один"}]}"""
    }
}
