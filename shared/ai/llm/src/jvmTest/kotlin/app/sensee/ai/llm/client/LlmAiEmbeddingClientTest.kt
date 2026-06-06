package app.sensee.ai.llm.client

import app.sensee.ai.llm.api.LlmHttpClientFactory
import app.sensee.ai.llm.config.AiCredentialsProvider
import app.sensee.ai.llm.config.LlmConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// jvmTest + runBlocking (not commonTest + runTest): the client factory installs
// Ktor's HttpTimeout plugin, whose real-time delay watcher is incompatible with
// runTest's virtual time — same constraint as LlmAiEnrichmentClientTest.
class LlmAiEmbeddingClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a well-formed response is mapped to an embedding`() =
        runBlocking {
            val engine =
                MockEngine {
                    respond(
                        content = """{"data":[{"embedding":[0.1,0.2,0.3]}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val embedding = client(engine, key = "sk-test").embed("hello")

            assertEquals(listOf(0.1f, 0.2f, 0.3f), embedding?.values?.toList())
            assertEquals(LlmAiEmbeddingClient.MODEL_REF, embedding?.model)
        }

    @Test
    fun `a missing api key returns null without calling the provider`() =
        runBlocking {
            var called = false
            val engine =
                MockEngine {
                    called = true
                    respond("{}")
                }

            assertNull(client(engine, key = null).embed("hello"))
            assertEquals(false, called)
        }

    @Test
    fun `a credentials failure returns null without calling the provider`() =
        runBlocking {
            var called = false
            val engine =
                MockEngine {
                    called = true
                    respond("{}")
                }
            val client =
                LlmAiEmbeddingClientFactory.create(
                    httpClient = LlmHttpClientFactory.create(engine = engine, json = json),
                    credentials = { error("vault unavailable") },
                    configProvider = { LlmConfig() },
                )

            assertNull(client.embed("hello"))
            assertEquals(false, called)
        }

    @Test
    fun `a rejected key returns null`() =
        runBlocking {
            val engine =
                MockEngine {
                    respond(
                        content = """{"error":"invalid_api_key"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            assertNull(client(engine, key = "sk-bad").embed("hello"))
        }

    @Test
    fun `blank text returns null without calling the provider`() =
        runBlocking {
            var called = false
            val engine =
                MockEngine {
                    called = true
                    respond("{}")
                }

            assertNull(client(engine, key = "sk-test").embed("   "))
            assertEquals(false, called)
        }

    @Test
    fun `the request carries the model, input, and dimension`() =
        runBlocking {
            var body = ""
            val engine =
                MockEngine { httpRequest ->
                    body = httpRequest.body.toByteArray().decodeToString()
                    respond(
                        content = """{"data":[{"embedding":[1.0]}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            client(engine, key = "sk-test").embed("hello")

            assertTrue(body.contains("\"model\":\"text-embedding-3-small\""))
            assertTrue(body.contains("\"input\":\"hello\""))
            assertTrue(body.contains("\"dimensions\":512"))
        }

    @Test
    fun `an empty data array returns null`() =
        runBlocking {
            val engine =
                MockEngine {
                    respond(
                        content = """{"data":[]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            assertNull(client(engine, key = "sk-test").embed("hello"))
        }

    @Test
    fun `an empty embedding array returns null`() =
        runBlocking {
            val engine =
                MockEngine {
                    respond(
                        content = """{"data":[{"embedding":[]}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            assertNull(client(engine, key = "sk-test").embed("hello"))
        }

    @Test
    fun `a malformed response body returns null`() =
        runBlocking {
            val engine =
                MockEngine {
                    respond(
                        content = "not json",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            assertNull(client(engine, key = "sk-test").embed("hello"))
        }

    private fun client(
        engine: MockEngine,
        key: String?,
    ) = LlmAiEmbeddingClientFactory.create(
        httpClient = LlmHttpClientFactory.create(engine = engine, json = json),
        credentials = AiCredentialsProvider { key },
        configProvider = { LlmConfig() },
    )
}
