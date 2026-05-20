package app.sensee.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// Integration test of the real client factory + safeBody mapping against MockEngine.
// jvmTest + runBlocking (not commonTest + runTest): these exercise Ktor's HttpTimeout
// plugin, whose real-time `delay` watcher is incompatible with runTest's virtual time
// (it would auto-advance and fire a spurious timeout). Real time + an instant MockEngine
// is deterministic; the timeout path is covered explicitly below with a tiny timeout.
class SafeBodyTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a successful json response is decoded`() =
        runBlocking {
            val client =
                NetworkHttpClientFactory.create(
                    engine =
                        MockEngine {
                            respond(
                                content = """{"id":"word-1","text":"hello","unknown":"ignored"}""",
                                status = HttpStatusCode.OK,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType,
                                        ContentType.Application.Json.toString(),
                                    ),
                            )
                        },
                    config = NetworkConfig(baseUrl = "https://fixture.test"),
                    json = json,
                )

            val result = safeBody<TestResponse> { client.get("words/word-1") }

            val success = assertIs<NetworkResult.Success<TestResponse>>(result)
            assertEquals(TestResponse(id = "word-1", text = "hello"), success.value)
        }

    @Test
    fun `an http not found is mapped to an http error`() =
        runBlocking {
            val client =
                NetworkHttpClientFactory.create(
                    engine =
                        MockEngine {
                            respond(
                                content = """{"error":"not_found"}""",
                                status = HttpStatusCode.NotFound,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType,
                                        ContentType.Application.Json.toString(),
                                    ),
                            )
                        },
                    config = NetworkConfig(baseUrl = "https://fixture.test"),
                    json = json,
                )

            val result = safeBody<TestResponse> { client.get("words/missing") }

            val failure = assertIs<NetworkResult.Failure>(result)
            val error = assertIs<NetworkError.Http>(failure.error)
            assertEquals(HttpStatusCode.NotFound.value, error.statusCode)
            assertEquals("""{"error":"not_found"}""", error.responseBody)
        }

    @Test
    fun `an http request timeout is mapped to a timeout error`() {
        runBlocking {
            val client =
                NetworkHttpClientFactory.create(
                    engine =
                        MockEngine {
                            // Far longer than the request timeout; the request is
                            // cancelled at ~50ms so the test does not actually wait.
                            delay(10_000)
                            respond(content = "{}", status = HttpStatusCode.OK)
                        },
                    config =
                        NetworkConfig(
                            baseUrl = "https://fixture.test",
                            requestTimeoutMillis = 50,
                        ),
                    json = json,
                )

            val result = safeBody<TestResponse> { client.get("words/slow") }

            val failure = assertIs<NetworkResult.Failure>(result)
            assertIs<NetworkError.Timeout>(failure.error)
        }
    }

    @Serializable
    private data class TestResponse(
        val id: String,
        val text: String,
    )
}
