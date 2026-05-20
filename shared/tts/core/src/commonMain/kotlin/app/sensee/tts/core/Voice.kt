package app.sensee.tts.core

import kotlin.jvm.JvmInline

@JvmInline
public value class VoiceId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "VoiceId must not be blank" }
    }
}

public data class Voice(
    val id: VoiceId,
    val locale: SpeechLocale,
    val displayName: String,
    val gender: VoiceGender = VoiceGender.Unspecified,
)

public enum class VoiceGender { Male, Female, Unspecified }
