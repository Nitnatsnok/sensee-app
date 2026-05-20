package app.sensee.tts.testKit

import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioFormat
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechSynthesizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Returns deterministic byte payloads keyed by [SpeechRequest], useful for
 * exercising flows that consume synthesized audio without hitting the network.
 */
public class FakeSpeechSynthesizer(
    private val bytesFor: (SpeechRequest) -> ByteArray = { it.text.encodeToByteArray() },
    private val format: AudioFormat = AudioFormat.Mp3,
) : SpeechSynthesizer {
    private val recorded = mutableListOf<SpeechRequest>()

    public val requests: List<SpeechRequest> get() = recorded.toList()

    override suspend fun synthesize(request: SpeechRequest): AudioClip {
        recorded += request
        return AudioClip(bytes = bytesFor(request), format = format)
    }

    override fun stream(request: SpeechRequest): Flow<ByteArray> {
        recorded += request
        return flowOf(bytesFor(request))
    }
}
