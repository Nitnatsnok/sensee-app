package app.sensee.tts.openai.speaker

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.logging.AppLogger
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.SpeechSynthesizer
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import app.sensee.tts.openai.synthesis.OpenAiSynthesizer
import app.sensee.tts.playback.AudioPlayer
import app.sensee.tts.playback.AudioPlayerFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Plays audio from a [SpeechSynthesizer] (an [OpenAiSynthesizer], optionally
 * wrapped in a caching decorator) through a platform [AudioPlayer]. Mirrors the
 * ElevenLabs speaker: a fresh player per request so overlapping `speak` calls
 * cancel cleanly.
 */
public class OpenAiSpeaker(
    private val synthesizer: SpeechSynthesizer,
    private val playerFactory: AudioPlayerFactory,
    private val scope: CoroutineScope,
    logger: AppLogger,
) : Speaker {
    private val log = logger.tag("OpenAiSpeaker")
    private var currentJob: Job? = null
    private var currentPlayer: AudioPlayer? = null

    override fun speak(request: SpeechRequest): SpeechHandle {
        stop()
        val handle = OpenAiSpeechHandle()
        val job =
            scope.launch {
                playSpeech(request, handle)
            }
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                handle.update(SpeechState.Failed(TtsError.Cancelled()))
                log.debug { "speak cancelled" }
            }
        }
        currentJob = job
        handle.attachJob(job)
        return handle
    }

    private suspend fun playSpeech(
        request: SpeechRequest,
        handle: OpenAiSpeechHandle,
    ) {
        val started = TimeSource.Monotonic.markNow()
        log.debug {
            "speak start: ${request.text.length} chars, " +
                "${request.locale.bcp47}, rate x${request.rate.multiplier}"
        }
        var player: AudioPlayer? = null
        try {
            runCatchingCancellable {
                handle.update(SpeechState.Loading)
                player = createAttachedPlayer(handle)
                handle.update(SpeechState.Speaking)
                val bytes = playTracedStream(player, request, started)
                handle.update(SpeechState.Done)
                log.debug { "speak done in ${started.elapsedNow()}, $bytes bytes" }
            }.onFailure { other ->
                handleSpeechFailure(other, handle, started)
            }
        } finally {
            releasePlayer(player, handle)
        }
    }

    private fun createAttachedPlayer(handle: OpenAiSpeechHandle): AudioPlayer =
        playerFactory.create().also {
            currentPlayer = it
            handle.attachPlayer(it)
        }

    private suspend fun playTracedStream(
        player: AudioPlayer,
        request: SpeechRequest,
        started: TimeMark,
    ): Long {
        var bytes = 0L
        var firstChunkSeen = false
        val traced =
            synthesizer.stream(request).onEach { chunk ->
                bytes += chunk.size
                if (!firstChunkSeen) {
                    firstChunkSeen = true
                    log.debug { "first audio chunk after ${started.elapsedNow()}" }
                }
            }
        player.playStream(traced, rateMultiplier = request.rate.multiplier)
        return bytes
    }

    private fun handleSpeechFailure(
        throwable: Throwable,
        handle: OpenAiSpeechHandle,
        started: TimeMark,
    ) {
        if (throwable is TtsException) {
            handle.update(SpeechState.Failed(throwable.error))
            log.warn(throwable) {
                "speak failed (${throwable.error}) after ${started.elapsedNow()}"
            }
        } else {
            handle.update(SpeechState.Failed(TtsError.Unknown(throwable.message)))
            log.error(throwable) {
                "speak crashed after ${started.elapsedNow()}: ${throwable.message ?: "no message"}"
            }
        }
    }

    private fun releasePlayer(
        player: AudioPlayer?,
        handle: OpenAiSpeechHandle,
    ) {
        player?.let {
            handle.detachPlayer(it)
            it.release()
        }
        if (currentPlayer === player) currentPlayer = null
    }

    override fun stop() {
        currentJob?.cancel()
        currentPlayer?.stop()
        currentJob = null
        currentPlayer = null
    }
}

private class OpenAiSpeechHandle : SpeechHandle {
    override val state: StateFlow<SpeechState>
        field = MutableStateFlow<SpeechState>(SpeechState.Idle)
    private var job: Job? = null
    private var player: AudioPlayer? = null

    fun update(next: SpeechState) {
        state.update { next }
    }

    fun attachJob(job: Job) {
        this.job = job
    }

    fun attachPlayer(player: AudioPlayer) {
        this.player = player
    }

    fun detachPlayer(player: AudioPlayer) {
        if (this.player === player) {
            this.player = null
        }
    }

    override fun cancel() {
        player?.stop()
        job?.cancel()
    }
}
