package app.sensee.tts.elevenlabs.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TextToSpeechRequestDto(
    val text: String,
    @SerialName("model_id") val modelId: String,
    @SerialName("voice_settings") val voiceSettings: VoiceSettingsDto,
)

@Serializable
internal data class VoiceSettingsDto(
    val stability: Double,
    @SerialName("similarity_boost") val similarityBoost: Double,
)

@Serializable
internal data class VoiceListResponseDto(
    val voices: List<RemoteVoiceDto>,
)

@Serializable
internal data class RemoteVoiceDto(
    @SerialName("voice_id") val voiceId: String,
    val name: String,
    val labels: Map<String, String> = emptyMap(),
)

@Serializable
internal data class RemoteModelDto(
    @SerialName("model_id") val modelId: String,
    val name: String = "",
)
