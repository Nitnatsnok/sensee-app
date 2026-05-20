package app.sensee.tts.elevenlabs.catalog

import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import app.sensee.tts.elevenlabs.config.StaticElevenLabsCredentialsProvider
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

// jvmTest + runBlocking: the network factory installs Ktor's HttpTimeout,
// incompatible with runTest virtual time (same constraint as the synthesizer test).
class ElevenLabsCatalogTest {
    private fun catalog(engine: MockEngine): ElevenLabsCatalog =
        ElevenLabsCatalogFactory.create(
            httpClient =
                NetworkHttpClientFactory.create(
                    engine = engine,
                    config = NetworkConfig(baseUrl = ElevenLabsConfig.DEFAULT_BASE_URL),
                    json = Json { ignoreUnknownKeys = true },
                ),
            config = ElevenLabsConfig(),
            credentials = StaticElevenLabsCredentialsProvider("xi-key"),
        )

    private fun json(body: String) =
        MockEngine {
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

    @Test
    fun `model and voice ids are extracted from the live listings`() =
        runBlocking {
            val models =
                catalog(
                    json("""[{"model_id":"eleven_multilingual_v2","name":"Multilingual v2"}]"""),
                ).modelIds()
            val voices =
                catalog(
                    json("""{"voices":[{"voice_id":"21m00","name":"Rachel"}]}"""),
                ).voiceIds()

            assertEquals(listOf("eleven_multilingual_v2"), models)
            assertEquals(listOf("21m00"), voices)
        }
}
