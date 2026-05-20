package app.sensee.tts.integration

import app.sensee.tts.core.TtsKeyCheck
import app.sensee.tts.core.TtsKeyVerificationRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The never-throw contract ProfileHomeLogic relies on by omitting try/catch:
 * verifyKey always returns a typed result, never throws — for an
 * empty key, an HTTP rejection, or a transport failure.
 */
class RoutingTtsCatalogTest {
    private class FakeTransportException : Exception("connection reset")

    private fun catalog(engine: MockEngine) = RoutingTtsCatalog(Json, engine)

    private suspend fun RoutingTtsCatalog.verifyElevenLabs(apiKey: String): TtsKeyCheck =
        verifyKey(TtsKeyVerificationRequest(providerId = "elevenlabs", apiKey = apiKey))

    @Test
    fun `a blank key is rejected without a network call`() =
        runTest {
            val engine = MockEngine { error("must not hit the network for a blank key") }
            val result = catalog(engine).verifyElevenLabs("   ")

            assertEquals(TtsKeyCheck.Invalid("API key is empty"), result)
        }

    @Test
    fun `an HTTP rejection becomes a typed Invalid rather than a thrown exception`() =
        runTest {
            val engine = MockEngine { respond("unauthorized", HttpStatusCode.Unauthorized) }
            val result = catalog(engine).verifyElevenLabs("el-bad")

            assertTrue(result is TtsKeyCheck.Invalid, "a rejected key never throws across the seam")
        }

    @Test
    fun `a transport failure becomes a typed Invalid rather than a thrown exception`() =
        runTest {
            val engine = MockEngine { throw FakeTransportException() }
            val result = catalog(engine).verifyElevenLabs("el-key")

            assertTrue(result is TtsKeyCheck.Invalid, "an unreachable provider never throws")
        }
}
