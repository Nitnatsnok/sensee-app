package app.sensee.tts.elevenlabs.synthesis

import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioFormat
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechSynthesizer
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import app.sensee.tts.elevenlabs.api.ElevenLabsApi
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import app.sensee.tts.elevenlabs.dto.TextToSpeechRequestDto
import app.sensee.tts.elevenlabs.dto.VoiceSettingsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

public class ElevenLabsSynthesizer internal constructor(
    private val api: ElevenLabsApi,
    private val config: ElevenLabsConfig,
) : SpeechSynthesizer {
    override suspend fun synthesize(request: SpeechRequest): AudioClip {
        val voiceId = request.resolveVoiceId(config)
        val body = request.toRequestBody(config)
        val bytes = api.synthesize(voiceId.value, body)
        return AudioClip(bytes = bytes, format = AudioFormat.Mp3)
    }

    override fun stream(request: SpeechRequest): Flow<ByteArray> =
        flow {
            val voiceId = request.resolveVoiceId(config)
            val body = request.toRequestBody(config)
            emitAll(api.stream(voiceId.value, body))
        }
}

private fun SpeechRequest.resolveVoiceId(config: ElevenLabsConfig): app.sensee.tts.core.VoiceId {
    voiceId?.let { return it }
    config.defaultVoiceByLocale[locale]?.let { return it }
    throw TtsException(
        TtsError.NoVoiceForLocale(
            locale = locale,
            message = "No ElevenLabs voice configured for ${locale.bcp47}",
        ),
    )
}

private fun SpeechRequest.toRequestBody(config: ElevenLabsConfig): TextToSpeechRequestDto =
    TextToSpeechRequestDto(
        text = text,
        modelId = modelId?.takeIf { it.isNotBlank() } ?: config.modelFor(quality),
        voiceSettings =
            VoiceSettingsDto(
                stability = config.stability,
                similarityBoost = config.similarityBoost,
            ),
    )
