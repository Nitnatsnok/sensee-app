package app.sensee.ai.llm.catalog

import app.sensee.ai.core.AiKeyCheck
import app.sensee.ai.llm.api.LlmHttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// jvmTest + runBlocking (not commonTest + runTest): the factory installs Ktor's
// HttpTimeout plugin, whose real-time delay watcher is incompatible with
// runTest's virtual time.
class LlmModelCatalogTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun catalog(engine: MockEngine) =
        LlmModelCatalogFactory.create(httpClient = LlmHttpClientFactory.create(engine = engine, json = json))

    private fun jsonOk(content: String) =
        MockEngine {
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

    @Test
    fun `a blank key is invalid without calling the provider`() =
        runBlocking {
            var called = false
            val engine =
                MockEngine {
                    called = true
                    respond("{}")
                }

            val result = catalog(engine).verifyKey("https://api.openai.com/", "  ")

            assertEquals(AiKeyCheck.Invalid("API key is empty"), result)
            assertEquals(false, called)
        }

    @Test
    fun `a valid key returns the chat-suitable models`() =
        runBlocking {
            val engine = jsonOk("""{"data":[{"id":"gpt-4o"},{"id":"o3-mini"}]}""")

            val result = catalog(engine).verifyKey("https://api.openai.com/", "sk-test")

            assertEquals(AiKeyCheck.Valid(listOf("gpt-4o", "o3-mini")), result)
        }

    @Test
    fun `non-chat models are filtered out of a valid listing`() =
        runBlocking {
            val engine =
                jsonOk(
                    """{"data":[{"id":"gpt-4o"},{"id":"whisper-1"},""" +
                        """{"id":"text-embedding-3-small"},{"id":"dall-e-3"},""" +
                        """{"id":"o3-mini"},{"id":"omni-moderation-latest"},""" +
                        """{"id":"codex-mini-latest"},{"id":"gpt-5-codex"},""" +
                        """{"id":"openai/gpt-4o-mini"}]}""",
                )

            val result = catalog(engine).verifyKey("https://api.openai.com/", "sk-test")

            assertEquals(AiKeyCheck.Valid(listOf("gpt-4o", "o3-mini", "openai/gpt-4o-mini")), result)
        }

    @Test
    fun `dated snapshots and superseded families are dropped while canonical aliases are kept`() =
        runBlocking {
            val engine =
                jsonOk(
                    """{"data":[{"id":"gpt-4o"},{"id":"gpt-4o-2024-05-13"},""" +
                        """{"id":"gpt-4-1106-preview"},{"id":"gpt-3.5-turbo"},""" +
                        """{"id":"gpt-4o-mini"},{"id":"o3-mini"},{"id":"o1-2024-12-17"},""" +
                        """{"id":"gpt-3.5-turbo-instruct"},{"id":"gpt-4-vision-preview"},""" +
                        """{"id":"gpt-4-turbo"},{"id":"gpt-4-turbo-preview"},""" +
                        """{"id":"gpt-4.5-preview"},{"id":"o1-preview"},{"id":"o1-mini"},""" +
                        """{"id":"openai/gpt-4o-mini"},{"id":"anthropic/claude-3.5-sonnet"}]}""",
                )

            val result = catalog(engine).verifyKey("https://api.openai.com/", "sk-test")

            assertEquals(
                AiKeyCheck.Valid(
                    listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet"),
                ),
                result,
            )
        }

    @Test
    fun `a valid key with no chat model is still valid with an empty list`() =
        runBlocking {
            val engine = jsonOk("""{"data":[{"id":"whisper-1"},{"id":"text-embedding-3-large"}]}""")

            val result = catalog(engine).verifyKey("https://api.openai.com/", "sk-test")

            assertEquals(AiKeyCheck.Valid(emptyList()), result)
        }

    @Test
    fun `a rejected key reports an invalid reason`() =
        runBlocking {
            val engine = MockEngine { respond("unauthorized", HttpStatusCode.Unauthorized) }

            val result = catalog(engine).verifyKey("https://api.openai.com/", "sk-bad")

            assertEquals(AiKeyCheck.Invalid("API key was rejected by the provider"), result)
        }

    @Test
    fun `an unreachable provider is invalid rather than a crash`() =
        runBlocking {
            val engine = MockEngine { throw java.io.IOException("connection refused") }

            val result = catalog(engine).verifyKey("https://api.openai.com/", "sk-test")

            val invalid = assertIs<AiKeyCheck.Invalid>(result)
            assertTrue(invalid.reason.startsWith("Provider unreachable"))
        }
}
