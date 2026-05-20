package app.sensee.tts.openai.speaker

import app.sensee.core.observability.logging.NoOpAppLogger
import app.sensee.tts.cache.CachingSpeechSynthesizer
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.SpeechLocale
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.playback.AudioPlayer
import app.sensee.tts.playback.AudioPlayerFactory
import app.sensee.tts.testKit.FakeSpeechSynthesizer
import app.sensee.tts.testKit.InMemoryAudioClipStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

private open class NoOpAudioPlayer : AudioPlayer {
    override suspend fun play(
        clip: AudioClip,
        rateMultiplier: Float,
    ) = Unit

    override fun stop() = Unit

    override fun release() = Unit
}

private object NoOpAudioPlayerFactory : AudioPlayerFactory {
    override fun create(): AudioPlayer = NoOpAudioPlayer()
}

class OpenAiSpeakerCachingTest {
    @Test
    fun `repeated request replays from cache without re-synthesizing`() =
        runTest {
            val delegate = FakeSpeechSynthesizer()
            val speaker =
                OpenAiSpeaker(
                    synthesizer =
                        CachingSpeechSynthesizer(
                            delegate = delegate,
                            store = InMemoryAudioClipStore(),
                            engineId = "openai:test",
                            logger = NoOpAppLogger,
                        ),
                    playerFactory = NoOpAudioPlayerFactory,
                    scope = backgroundScope,
                    logger = NoOpAppLogger,
                )
            val request = SpeechRequest(text = "hello", locale = SpeechLocale.English)

            speaker.speak(request).awaitTerminal()
            speaker.speak(request).awaitTerminal()

            assertEquals(
                1,
                delegate.requests.size,
                "second playback must hit the cache, not the network synthesizer",
            )
        }

    @Test
    fun `canceling handle stops current audio player`() =
        runTest {
            val player = BlockingAudioPlayer()
            val speaker =
                OpenAiSpeaker(
                    synthesizer = FakeSpeechSynthesizer(),
                    playerFactory =
                        object : AudioPlayerFactory {
                            override fun create(): AudioPlayer = player
                        },
                    scope = backgroundScope,
                    logger = NoOpAppLogger,
                )

            val handle = speaker.speak(SpeechRequest(text = "hello", locale = SpeechLocale.English))
            player.started.await()
            handle.cancel()

            withTimeout(1_000) { player.stopped.await() }
        }

    private suspend fun app.sensee.tts.core.SpeechHandle.awaitTerminal() {
        state.first { it is SpeechState.Done || it is SpeechState.Failed }
    }
}

private class BlockingAudioPlayer : NoOpAudioPlayer() {
    val started = CompletableDeferred<Unit>()
    val stopped = CompletableDeferred<Unit>()

    override suspend fun playStream(
        chunks: kotlinx.coroutines.flow.Flow<ByteArray>,
        rateMultiplier: Float,
    ) {
        started.complete(Unit)
        awaitCancellation()
    }

    override fun stop() {
        stopped.complete(Unit)
    }
}
