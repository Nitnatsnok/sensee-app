package app.sensee.ai.llm.client

import app.sensee.ai.core.contract.AiEnrichmentExtension
import app.sensee.ai.core.contract.EnrichmentAvailability
import app.sensee.ai.core.request.DefaultEnrichmentRequestModifiers
import app.sensee.ai.core.request.EnrichmentRequest
import app.sensee.ai.core.request.EnrichmentSchema
import app.sensee.ai.core.request.SenseCoverage
import app.sensee.ai.core.request.UserEnrichmentPreferences
import app.sensee.ai.core.request.UserEnrichmentPreferencesProvider
import app.sensee.ai.core.wire.EnrichmentResponseV1
import app.sensee.ai.llm.api.LlmHttpClientFactory
import app.sensee.ai.llm.config.LlmConfig
import app.sensee.grammar.domain.TaxonomyInvariants
import app.sensee.grammar.domain.TaxonomyInvariantsProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// jvmTest + runBlocking (not commonTest + runTest): the client factory installs
// Ktor's HttpTimeout plugin, whose real-time delay watcher is incompatible with
// runTest's virtual time — same constraint as ElevenLabsSynthesizerTest.
class LlmAiEnrichmentClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val request = EnrichmentRequest(term = "run")

    @Test
    fun `missing api key degrades to unavailable without calling the provider`() =
        runBlocking {
            var called = false
            val engine =
                MockEngine {
                    called = true
                    respond("{}")
                }
            val client =
                LlmAiEnrichmentClientFactory.create(
                    httpClient = llmHttpClient(engine),
                    credentials = { null },
                    configProvider = { LlmConfig() },
                    taxonomyInvariantsProvider = NoTaxonomyProvider,
                    modifiers = DefaultEnrichmentRequestModifiers,
                    preferencesProvider = NoOpPreferencesProvider,
                    json = json,
                )

            val result = client.enrich(request)

            assertTrue(result.availability is EnrichmentAvailability.Unavailable)
            assertEquals(false, called)
        }

    @Test
    fun `credentials failure degrades to unavailable without calling the provider`() =
        runBlocking {
            var called = false
            val engine =
                MockEngine {
                    called = true
                    respond("{}")
                }
            val client =
                LlmAiEnrichmentClientFactory.create(
                    httpClient = llmHttpClient(engine),
                    credentials = { error("vault unavailable") },
                    configProvider = { LlmConfig() },
                    taxonomyInvariantsProvider = NoTaxonomyProvider,
                    modifiers = DefaultEnrichmentRequestModifiers,
                    preferencesProvider = NoOpPreferencesProvider,
                    json = json,
                )

            val result = client.enrich(request)

            assertTrue(result.availability is EnrichmentAvailability.Unavailable)
            assertEquals(false, called)
        }

    @Test
    fun `well-formed provider answer is mapped through the seam`() =
        runBlocking {
            val content = """{"version": 1,"items":[{"translation":"бежать","definition":"to move fast"}]}"""
            val engine =
                MockEngine {
                    val encoded = json.encodeToString(content)
                    respond(
                        content = """{"choices":[{"message":{"role":"assistant","content":$encoded}}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client =
                LlmAiEnrichmentClientFactory.create(
                    httpClient = llmHttpClient(engine),
                    credentials = { "sk-test" },
                    configProvider = { LlmConfig() },
                    taxonomyInvariantsProvider = NoTaxonomyProvider,
                    modifiers = DefaultEnrichmentRequestModifiers,
                    preferencesProvider = NoOpPreferencesProvider,
                    json = json,
                )

            val result = client.enrich(request)

            assertEquals(EnrichmentAvailability.Available, result.availability)
            assertEquals(listOf("бежать"), result.suggestions.map { it.translation })
        }

    @Test
    fun `default config requests a plain json_object response format`() =
        runBlocking {
            var body = ""
            val engine =
                MockEngine { httpRequest ->
                    body = httpRequest.body.toByteArray().decodeToString()
                    respondCompletion()
                }
            val client =
                LlmAiEnrichmentClientFactory.create(
                    httpClient = llmHttpClient(engine),
                    credentials = { "sk-test" },
                    configProvider = { LlmConfig() },
                    taxonomyInvariantsProvider = NoTaxonomyProvider,
                    modifiers = DefaultEnrichmentRequestModifiers,
                    preferencesProvider = NoOpPreferencesProvider,
                    json = json,
                )

            client.enrich(request)

            assertTrue(body.contains("\"response_format\":{\"type\":\"json_object\"}"))
        }

    @Test
    fun `structured output config sends the schema as a json_schema response format`() =
        runBlocking {
            var body = ""
            val engine =
                MockEngine { httpRequest ->
                    body = httpRequest.body.toByteArray().decodeToString()
                    respondCompletion()
                }
            val client =
                LlmAiEnrichmentClientFactory.create(
                    httpClient = llmHttpClient(engine),
                    credentials = { "sk-test" },
                    configProvider = { LlmConfig(structuredOutput = true) },
                    taxonomyInvariantsProvider = NoTaxonomyProvider,
                    modifiers = DefaultEnrichmentRequestModifiers,
                    preferencesProvider = NoOpPreferencesProvider,
                    json = json,
                )

            client.enrich(request)

            assertTrue(body.contains("\"type\":\"json_schema\""))
            assertTrue(body.contains("\"name\":\"enrichment_response\""))
            assertTrue(body.contains("\"surface_form\""))
        }

    @Test
    fun `structured output schema carries taxonomy-driven unit_type enum`() =
        runBlocking {
            var body = ""
            val engine =
                MockEngine { httpRequest ->
                    body = httpRequest.body.toByteArray().decodeToString()
                    respondCompletion()
                }
            val invariants =
                TaxonomyInvariants.EMPTY.copy(knownUnitTypeIds = setOf("noun", "verb"))
            val client =
                LlmAiEnrichmentClientFactory.create(
                    httpClient = llmHttpClient(engine),
                    credentials = { "sk-test" },
                    configProvider = { LlmConfig(structuredOutput = true) },
                    taxonomyInvariantsProvider = TaxonomyInvariantsProvider { invariants },
                    modifiers = DefaultEnrichmentRequestModifiers,
                    preferencesProvider = NoOpPreferencesProvider,
                    json = json,
                )

            client.enrich(request)

            assertTrue(body.contains("\"enum\":[\"noun\",\"verb\"]"))
        }

    @Test
    fun `extension fields are requested in schema and returned on suggestions`() =
        runBlocking {
            var body = ""
            val extension =
                object : AiEnrichmentExtension {
                    override val id: String = "etymology"
                    override val ownedKeys: Set<String> = setOf("etymology")

                    override fun fields(): List<EnrichmentSchema.Field> =
                        listOf(
                            EnrichmentSchema.Field(
                                serialName = "etymology",
                                shape = EnrichmentSchema.ShapeType.Text,
                                guidance = "short origin note",
                            ),
                        )
                }
            val content = """{"version": 1,"items":[{"translation":"бежать","etymology":"Old English"}]}"""
            val engine =
                MockEngine { httpRequest ->
                    body = httpRequest.body.toByteArray().decodeToString()
                    val encoded = json.encodeToString(content)
                    respond(
                        content = """{"choices":[{"message":{"role":"assistant","content":$encoded}}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client =
                LlmAiEnrichmentClientFactory.create(
                    httpClient = llmHttpClient(engine),
                    credentials = { "sk-test" },
                    configProvider = { LlmConfig(structuredOutput = true) },
                    taxonomyInvariantsProvider = NoTaxonomyProvider,
                    modifiers = DefaultEnrichmentRequestModifiers,
                    preferencesProvider = NoOpPreferencesProvider,
                    json = json,
                    extensions = setOf(extension),
                )

            val result =
                client.enrich(EnrichmentRequest(term = "run", senseCoverage = SenseCoverage.Minimal))

            assertTrue(body.contains("\"etymology\""))
            val etymology =
                result
                    .suggestions
                    .single()
                    .extensions["etymology"]
                    ?.jsonPrimitive
                    ?.content
            assertEquals(
                "Old English",
                etymology,
            )
        }

    @Test
    fun `an extension claiming a built-in key cannot leak into the suggestion bucket`() =
        runBlocking {
            val rogue =
                object : AiEnrichmentExtension {
                    override val id: String = "rogue"
                    override val ownedKeys: Set<String> = setOf("translation")

                    override fun fields(): List<EnrichmentSchema.Field> = emptyList()
                }
            val content = """{"version": 1,"items":[{"translation":"бежать"}]}"""
            val engine =
                MockEngine {
                    respond(
                        content = """{"choices":[{"message":{"role":"assistant","content":${json.encodeToString(
                            content,
                        )}}}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client =
                LlmAiEnrichmentClientFactory.create(
                    httpClient = llmHttpClient(engine),
                    credentials = { "sk-test" },
                    configProvider = { LlmConfig() },
                    taxonomyInvariantsProvider = NoTaxonomyProvider,
                    modifiers = DefaultEnrichmentRequestModifiers,
                    preferencesProvider = NoOpPreferencesProvider,
                    json = json,
                    extensions = setOf(rogue),
                )

            val suggestion =
                client
                    .enrich(EnrichmentRequest(term = "run", senseCoverage = SenseCoverage.Minimal))
                    .suggestions
                    .single()

            assertEquals("бежать", suggestion.translation)
            assertTrue(suggestion.extensions.isEmpty(), "built-in keys never reach the opaque bucket")
        }

    @Test
    fun `single sense under common coverage triggers one corrective retry`() =
        runBlocking {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respondItems(if (calls == 1) listOf("первый") else listOf("a", "b", "c"))
                }
            val client = client(engine)

            val result = client.enrich(EnrichmentRequest(term = "come across"))

            assertEquals(2, calls)
            assertEquals(listOf("a", "b", "c"), result.suggestions.map { it.translation })
        }

    @Test
    fun `minimal coverage does not retry a single-sense answer`() =
        runBlocking {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respondItems(listOf("один"))
                }
            val client = client(engine)

            val result =
                client.enrich(
                    EnrichmentRequest(term = "look after", senseCoverage = SenseCoverage.Minimal),
                )

            assertEquals(1, calls)
            assertEquals(listOf("один"), result.suggestions.map { it.translation })
        }

    @Test
    fun `a genuine single sense is kept when the retry adds nothing`() =
        runBlocking {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respondItems(listOf("единственный"))
                }
            val client = client(engine)

            val result = client.enrich(EnrichmentRequest(term = "a piece of cake"))

            assertEquals(2, calls)
            assertEquals(listOf("единственный"), result.suggestions.map { it.translation })
        }

    @Test
    fun `a multi-sense first answer is not retried`() =
        runBlocking {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respondItems(listOf("раз", "два"))
                }
            val client = client(engine)

            val result = client.enrich(EnrichmentRequest(term = "come across"))

            assertEquals(1, calls)
            assertEquals(listOf("раз", "два"), result.suggestions.map { it.translation })
        }

    @Test
    fun `the prompt asks for the sense inventory and hints come across on retry`() =
        runBlocking {
            val bodies = mutableListOf<String>()
            val engine =
                MockEngine { httpRequest ->
                    bodies += httpRequest.body.toByteArray().decodeToString()
                    respondItems(listOf("only"))
                }
            val client = client(engine)

            client.enrich(EnrichmentRequest(term = "come across"))

            assertTrue(bodies[0].contains("sense inventory"))
            assertTrue(bodies[0].contains("Coverage: usually return 2-5"))
            assertTrue(bodies[1].contains("returned only one sense"))
            assertTrue(bodies[1].contains("come across as <adjective/noun>"))
        }

    @Test
    fun `topic preferences are injected into the prompt`() =
        runBlocking {
            var body = ""
            val engine =
                MockEngine { httpRequest ->
                    body = httpRequest.body.toByteArray().decodeToString()
                    respondCompletion()
                }

            client(engine).enrich(
                EnrichmentRequest(
                    term = "run",
                    topicPreferences = listOf("travel and tourism", "food and cooking"),
                ),
            )

            assertTrue(body.contains("The learner is interested in these topics"))
            assertTrue(body.contains("travel and tourism, food and cooking"))
        }

    @Test
    fun `an empty topic preference list leaves the prompt without topic guidance`() =
        runBlocking {
            var body = ""
            val engine =
                MockEngine { httpRequest ->
                    body = httpRequest.body.toByteArray().decodeToString()
                    respondCompletion()
                }

            client(engine).enrich(EnrichmentRequest(term = "run"))

            assertTrue(!body.contains("The learner is interested in these topics"))
        }

    private fun llmHttpClient(engine: MockEngine) = LlmHttpClientFactory.create(engine = engine, json = json)

    private fun client(engine: MockEngine) =
        LlmAiEnrichmentClientFactory.create(
            httpClient = llmHttpClient(engine),
            credentials = { "sk-test" },
            configProvider = { LlmConfig() },
            taxonomyInvariantsProvider = NoTaxonomyProvider,
            modifiers = DefaultEnrichmentRequestModifiers,
            preferencesProvider = NoOpPreferencesProvider,
            json = json,
        )

    private fun MockRequestHandleScope.respondItems(translations: List<String>) =
        respond(
            content = """{"choices":[{"message":{"role":"assistant","content":${
                json.encodeToString(
                    buildString {
                        append("{\"version\":${EnrichmentResponseV1.SCHEMA_VERSION},\"items\":[")
                        append(translations.joinToString(",") { "{\"translation\":\"$it\"}" })
                        append("]}")
                    },
                )
            }}}]}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    private fun MockRequestHandleScope.respondCompletion() = respondItems(listOf("x"))
}

private val NoTaxonomyProvider: TaxonomyInvariantsProvider = TaxonomyInvariantsProvider { null }

private val NoOpPreferencesProvider: UserEnrichmentPreferencesProvider =
    UserEnrichmentPreferencesProvider { UserEnrichmentPreferences.EMPTY }
