package app.sensee.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// jvmTest + runBlocking (not commonTest + runTest): the factory installs Ktor's
// HttpTimeout plugin, whose real-time delay watcher fights runTest's virtual time —
// same constraint as SafeBodyTest.
class NetworkHttpClientFactoryLoggingTest {
    private fun respondingEngine() =
        MockEngine {
            respond(
                content = """{"answer":"pong"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

    @Test
    fun `body log level logs the request body but redacts the bearer token`() =
        runBlocking {
            val logLines = mutableListOf<String>()
            val client =
                NetworkHttpClientFactory.create(
                    engine = respondingEngine(),
                    config = NetworkConfig(baseUrl = "https://example.test/"),
                    json = Json,
                    logger = { logLines += it },
                    logBodies = true,
                )

            client.post("https://example.test/ping") {
                header(HttpHeaders.Authorization, "Bearer sk-secret-123")
                contentType(ContentType.Application.Json)
                setBody("""{"ping":true}""")
            }

            val log = logLines.joinToString("\n")
            assertTrue(log.contains("\"ping\":true"), "the request body is logged at BODY level")
            assertFalse(log.contains("sk-secret-123"), "the bearer token must be redacted from the log")
        }

    @Test
    fun `the default info level does not log request bodies`() =
        runBlocking {
            val logLines = mutableListOf<String>()
            val client =
                NetworkHttpClientFactory.create(
                    engine = respondingEngine(),
                    config = NetworkConfig(baseUrl = "https://example.test/"),
                    json = Json,
                    logger = { logLines += it },
                )

            client.post("https://example.test/ping") {
                contentType(ContentType.Application.Json)
                setBody("""{"ping":true}""")
            }

            assertFalse(
                logLines.joinToString("\n").contains("\"ping\":true"),
                "the default INFO level must not log request bodies",
            )
        }
}
