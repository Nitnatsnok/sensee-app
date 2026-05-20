package app.sensee.tts.core

public sealed interface TtsError {
    public val message: String?

    public data class NoVoiceForLocale(
        val locale: SpeechLocale,
        override val message: String? = null,
    ) : TtsError

    public data class Network(
        override val message: String?,
        val statusCode: Int? = null,
    ) : TtsError

    public data class Quota(
        override val message: String? = null,
    ) : TtsError

    public data class Unsupported(
        override val message: String? = null,
    ) : TtsError

    public data class Cancelled(
        override val message: String? = null,
    ) : TtsError

    public data class Unknown(
        override val message: String?,
    ) : TtsError
}

public class TtsException(
    public val error: TtsError,
    cause: Throwable? = null,
) : Exception(error.message ?: error.toString(), cause)
