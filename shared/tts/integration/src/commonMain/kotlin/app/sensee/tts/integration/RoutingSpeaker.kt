package app.sensee.tts.integration

import app.sensee.core.coroutines.AppCoroutineScopes
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.logging.AppLogger
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.TtsError
import app.sensee.tts.elevenlabs.speaker.ElevenLabsSpeaker
import app.sensee.tts.openai.speaker.OpenAiSpeaker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The bound [Speaker] — pure routing logic. It receives the already-assembled
 * provider adapters as collaborators (built by [TtsIntegrationProviders], the
 * DI/composition layer, like the AI seam injects its LLM/fixture clients) and
 * only chooses one by the observed config: ElevenLabs (needs provider + key +
 * voice), OpenAI (provider + key), otherwise the always-available system
 * speaker. `speak` is non-suspending, so routing happens inside the returned
 * handle's job after reading the current persisted settings.
 */
public class RoutingSpeaker(
    private val elevenLabs: ElevenLabsSpeaker,
    private val openAi: OpenAiSpeaker,
    private val system: Speaker,
    private val settings: UserSettingsRepository,
    scopes: AppCoroutineScopes,
    logger: AppLogger,
) : Speaker {
    private val log = logger.tag("TtsRouting")
    private val scope = scopes.applicationScope
    private var currentJob: Job? = null
    private var currentDelegate: SpeechHandle? = null

    override fun speak(request: SpeechRequest): SpeechHandle {
        stop()
        val handle = RoutingSpeechHandle()
        val job =
            scope.launch {
                var delegate: SpeechHandle?
                handle.update(SpeechState.Loading)
                runCatchingCancellable {
                    ensureActive()
                    delegate = route(settings.readSettings().ai, request)
                    ensureActive()
                    currentDelegate = delegate
                    handle.attachDelegate(delegate)
                    delegate
                        .state
                        .filter { it != SpeechState.Idle }
                        .onEach { handle.update(it) }
                        .first { it is SpeechState.Done || it is SpeechState.Failed }
                }.onFailure { throwable ->
                    handle.update(SpeechState.Failed(TtsError.Unknown(throwable.message)))
                    log.error(throwable) { "route failed: ${throwable.message ?: "no message"}" }
                }
            }
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                handle.cancelDelegate()
                handle.update(SpeechState.Failed(TtsError.Cancelled()))
            }
        }
        currentJob = job
        handle.attachJob(job)
        return handle
    }

    private fun route(
        current: AiSettings,
        request: SpeechRequest,
    ): SpeechHandle =
        when {
            shouldUseElevenLabs(current) -> {
                log.debug {
                    "route → elevenlabs (voice=${current.ttsVoiceId ?: "default"}, " +
                        "model=${current.ttsModel ?: "default"})"
                }
                elevenLabs.speak(
                    request.copy(
                        voiceId = routedVoiceId(current, request.voiceId),
                        modelId = routedModelId(current, request.modelId),
                    ),
                )
            }
            shouldUseOpenAi(current) -> {
                log.debug {
                    "route → openai (voice=${current.ttsVoiceId ?: "default"}, " +
                        "model=${current.ttsModel ?: "default"})"
                }
                openAi.speak(
                    request.copy(
                        voiceId = routedVoiceId(current, request.voiceId),
                        modelId = routedModelId(current, request.modelId),
                    ),
                )
            }
            else -> {
                log.debug {
                    "route → system (provider=${current.ttsProvider}, " +
                        "hasKey=${!current.ttsApiKey.isNullOrBlank()})"
                }
                system.speak(request)
            }
        }

    override fun stop() {
        currentJob?.cancel()
        currentDelegate?.cancel()
        elevenLabs.stop()
        openAi.stop()
        system.stop()
        currentJob = null
        currentDelegate = null
    }
}

private class RoutingSpeechHandle : SpeechHandle {
    override val state: StateFlow<SpeechState>
        field = MutableStateFlow<SpeechState>(SpeechState.Idle)
    private var job: Job? = null
    private var delegate: SpeechHandle? = null

    fun update(next: SpeechState) {
        state.update { next }
    }

    fun attachJob(job: Job) {
        this.job = job
    }

    fun attachDelegate(delegate: SpeechHandle) {
        this.delegate = delegate
    }

    fun cancelDelegate() {
        delegate?.cancel()
    }

    override fun cancel() {
        cancelDelegate()
        job?.cancel()
    }
}
