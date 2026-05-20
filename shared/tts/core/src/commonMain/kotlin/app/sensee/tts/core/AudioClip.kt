package app.sensee.tts.core

public data class AudioClip(
    val bytes: ByteArray,
    val format: AudioFormat,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioClip) return false
        return format == other.format && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * format.hashCode() + bytes.contentHashCode()
}

public enum class AudioFormat {
    Mp3,
    Pcm16,
    Wav,
}
