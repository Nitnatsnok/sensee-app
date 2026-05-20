package app.sensee.tts.core

/**
 * Resolves the voice to use for a given locale when [SpeechRequest.voiceId] is
 * null. Each engine can plug in its own catalog (preset voice ids for
 * ElevenLabs, system voice ids for the platform engine).
 */
public interface VoiceCatalog {
    public suspend fun list(): List<Voice>

    public suspend fun defaultVoice(locale: SpeechLocale): Voice?
}
