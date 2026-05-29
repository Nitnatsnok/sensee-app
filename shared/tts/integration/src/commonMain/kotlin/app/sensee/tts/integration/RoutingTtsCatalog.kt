package app.sensee.tts.integration

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.tts.core.TtsCatalog
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import app.sensee.tts.core.TtsKeyCheck
import app.sensee.tts.core.TtsKeyVerificationRequest
import app.sensee.tts.elevenlabs.catalog.ElevenLabsCatalogFactory
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import app.sensee.tts.elevenlabs.config.StaticElevenLabsCredentialsProvider
import io.ktor.client.HttpClient

/**
 * The bound [TtsCatalog]. Provider-specific catalog calls stay here, in the
 * integration layer; the core seam only carries a neutral provider id + key
 * request. OpenAI TTS keys are OpenAI-compatible and are verified through the
 * AI seam instead (handled by the settings logic).
 *
 * Pure: the [HttpClient] is supplied by [TtsIntegrationProviders] (the DI layer
 * owns network wiring, like the AI seam), so tests can pass a client built on a
 * MockEngine.
 */
public class RoutingTtsCatalog(
    private val httpClient: HttpClient,
    private val config: ElevenLabsConfig,
) : TtsCatalog {
    private companion object {
        const val ELEVEN_LABS_PROVIDER_ID = "elevenlabs"
    }

    override suspend fun verifyKey(request: TtsKeyVerificationRequest): TtsKeyCheck {
        if (request.providerId != ELEVEN_LABS_PROVIDER_ID) {
            return TtsKeyCheck.Invalid("Unsupported TTS provider: ${request.providerId}")
        }
        val apiKey = request.apiKey
        if (apiKey.isBlank()) {
            return TtsKeyCheck.Invalid("API key is empty")
        }
        val catalog =
            ElevenLabsCatalogFactory.create(
                httpClient = httpClient,
                config = config,
                credentials = StaticElevenLabsCredentialsProvider(apiKey),
            )
        return runCatchingCancellable {
            TtsKeyCheck.Valid(
                models = catalog.modelIds(),
                voices = catalog.voiceIds(),
            )
        }.getOrElse { throwable ->
            TtsKeyCheck.Invalid(ttsKeyCheckReason(throwable))
        }
    }
}

/**
 * Classifies a verify failure into a learner-facing reason. Pure so the
 * never-throw contract's branching (rejected key vs quota vs unreachable) is
 * unit-tested without standing up an HTTP client.
 */
internal fun ttsKeyCheckReason(throwable: Throwable): String {
    val error = (throwable as? TtsException)?.error
    return when {
        error is TtsError.Network && (error.statusCode == 401 || error.statusCode == 403) ->
            "API key was rejected by the provider"
        error is TtsError.Quota -> "ElevenLabs quota exceeded"
        else -> "Provider unreachable: ${throwable.message ?: throwable::class.simpleName}"
    }
}
