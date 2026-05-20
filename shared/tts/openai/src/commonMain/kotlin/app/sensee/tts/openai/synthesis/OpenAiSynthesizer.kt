package app.sensee.tts.openai.synthesis

import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.AudioFormat
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechSynthesizer
import app.sensee.tts.openai.api.OpenAiSpeechApi
import app.sensee.tts.openai.config.OpenAiSpeechConfig
import app.sensee.tts.openai.dto.SpeechRequestDto
import kotlinx.coroutines.flow.Flow

public class OpenAiSynthesizer internal constructor(
    private val api: OpenAiSpeechApi,
    private val config: OpenAiSpeechConfig,
) : SpeechSynthesizer {
    override suspend fun synthesize(request: SpeechRequest): AudioClip {
        val bytes = api.synthesize(request.toRequestBody(config))
        return AudioClip(bytes = bytes, format = AudioFormat.Mp3)
    }

    override fun stream(request: SpeechRequest): Flow<ByteArray> = api.stream(request.toRequestBody(config))
}

private fun SpeechRequest.toRequestBody(config: OpenAiSpeechConfig): SpeechRequestDto =
    SpeechRequestDto(
        model = modelId?.takeIf { it.isNotBlank() } ?: config.model,
        input = text,
        voice = voiceId?.value?.takeIf { it.isNotBlank() } ?: config.defaultVoice,
        responseFormat = config.responseFormat,
    )
