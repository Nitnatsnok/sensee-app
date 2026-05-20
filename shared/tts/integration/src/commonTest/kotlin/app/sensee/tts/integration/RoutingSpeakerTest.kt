package app.sensee.tts.integration

import app.sensee.core.coroutines.AppCoroutineScopes
import app.sensee.core.observability.logging.NoOpAppLogger
import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.TtsProvider
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioFormat
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechLocale
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.SpeechSynthesizer
import app.sensee.tts.core.TtsError
import app.sensee.tts.elevenlabs.speaker.ElevenLabsSpeaker
import app.sensee.tts.openai.speaker.OpenAiSpeaker
import app.sensee.tts.playback.AudioPlayer
import app.sensee.tts.playback.AudioPlayerFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingSpeakerTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `canceling the routing handle cancels the selected provider handle`() =
        runTest {
            val player = RecordingAudioPlayer()
            val openAiSpeaker =
                OpenAiSpeaker(
                    synthesizer = StaticSynthesizer,
                    playerFactory = FixedPlayerFactory(player),
                    scope = backgroundScope,
                    logger = NoOpAppLogger,
                )
            val routing =
                RoutingSpeaker(
                    elevenLabs =
                        ElevenLabsSpeaker(
                            synthesizer = StaticSynthesizer,
                            playerFactory = NewPlayerFactory,
                            scope = backgroundScope,
                            logger = NoOpAppLogger,
                        ),
                    openAi = openAiSpeaker,
                    system = NoopSpeaker,
                    settings =
                        FakeSettings(
                            AiSettings(
                                ttsProvider = TtsProvider.OpenAi,
                                ttsApiKey = "sk-openai",
                            ),
                        ),
                    scopes = AppCoroutineScopes(backgroundScope),
                    logger = NoOpAppLogger,
                )

            val handle = routing.speak(SpeechRequest(text = "hello", locale = SpeechLocale.English))
            player.started.await()

            handle.cancel()
            player.releaseCompleted.await()
            advanceUntilIdle()

            assertTrue(player.released)
            assertEquals(SpeechState.Failed(TtsError.Cancelled()), handle.state.value)
        }

    private object StaticSynthesizer : SpeechSynthesizer {
        override suspend fun synthesize(request: SpeechRequest): AudioClip =
            AudioClip(bytes = byteArrayOf(1), format = AudioFormat.Mp3)

        override fun stream(request: SpeechRequest): Flow<ByteArray> = flowOf(byteArrayOf(1))
    }

    private class RecordingAudioPlayer : AudioPlayer {
        val started = CompletableDeferred<Unit>()
        val releaseCompleted = CompletableDeferred<Unit>()
        var released = false

        override suspend fun play(
            clip: AudioClip,
            rateMultiplier: Float,
        ) {
            started.complete(Unit)
            awaitCancellation()
        }

        override suspend fun playStream(
            chunks: Flow<ByteArray>,
            rateMultiplier: Float,
        ) {
            started.complete(Unit)
            awaitCancellation()
        }

        override fun stop(): Unit = Unit

        override fun release() {
            released = true
            releaseCompleted.complete(Unit)
        }
    }

    private class FixedPlayerFactory(
        private val player: AudioPlayer,
    ) : AudioPlayerFactory {
        override fun create(): AudioPlayer = player
    }

    private object NewPlayerFactory : AudioPlayerFactory {
        override fun create(): AudioPlayer = RecordingAudioPlayer()
    }

    private class FakeSettings(
        ai: AiSettings,
    ) : UserSettingsRepository {
        private val snapshot = UserSettingsSnapshot(ai = ai)

        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(snapshot)

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = snapshot

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = error("unused")
    }

    private object NoopSpeaker : Speaker {
        override fun speak(request: SpeechRequest): SpeechHandle =
            object : SpeechHandle {
                override val state: MutableStateFlow<SpeechState> = MutableStateFlow(SpeechState.Done)

                override fun cancel(): Unit = Unit
            }

        override fun stop(): Unit = Unit
    }
}
