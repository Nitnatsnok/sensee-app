package app.sensee.tts.core

import kotlinx.coroutines.flow.Flow

/**
 * Produces audio bytes for a [SpeechRequest] without playing them.
 *
 * Implemented by engines that return audio (e.g. ElevenLabs); the system TTS
 * engine implements [Speaker] directly because it does its own playback.
 *
 * The eventual backend-proxied implementation will also satisfy this interface,
 * so consumers should depend on [SpeechSynthesizer] rather than any concrete
 * vendor type.
 */
public interface SpeechSynthesizer {
    public suspend fun synthesize(request: SpeechRequest): AudioClip

    /**
     * Streams audio chunks as they become available. Implementations that don't
     * support streaming may emit a single chunk equal to [synthesize] output.
     */
    public fun stream(request: SpeechRequest): Flow<ByteArray>
}
