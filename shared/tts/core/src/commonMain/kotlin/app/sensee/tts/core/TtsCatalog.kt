package app.sensee.tts.core

/**
 * Verifies a TTS provider key and, on success, returns the provider's live
 * models/voices for settings pickers. The key is passed explicitly — the user
 * verifies before saving, so it cannot be read from saved settings. Never
 * throws: a rejected or unreachable provider is a typed [TtsKeyCheck.Invalid].
 */
public interface TtsCatalog {
    public suspend fun verifyKey(request: TtsKeyVerificationRequest): TtsKeyCheck
}

public data class TtsKeyVerificationRequest(
    val providerId: String,
    val apiKey: String,
)

/**
 * Outcome of verifying a TTS key. [Valid] carries the live models and voices;
 * [Invalid] carries a human-readable reason for the settings UI.
 */
public sealed interface TtsKeyCheck {
    public data class Valid(
        val models: List<String>,
        val voices: List<String>,
    ) : TtsKeyCheck

    public data class Invalid(
        val reason: String,
    ) : TtsKeyCheck
}
