package app.sensee.tts.core

import kotlin.jvm.JvmInline

@JvmInline
public value class SpeechLocale(
    public val bcp47: String,
) {
    init {
        require(bcp47.isNotBlank()) { "SpeechLocale tag must not be blank" }
    }

    public companion object {
        public val English: SpeechLocale = SpeechLocale("en-US")
        public val Russian: SpeechLocale = SpeechLocale("ru-RU")
    }
}
