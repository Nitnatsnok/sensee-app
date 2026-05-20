package app.sensee.core.secureStorage

import kotlin.jvm.JvmInline

/**
 * Typed handle for a slot in [SecureStorage]. Wrapping the raw string in a
 * value class keeps the per-platform vault keyed by a single namespace and
 * makes it grep-able which fields are secrets.
 */
@JvmInline
public value class SecureStorageKey(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "SecureStorageKey must not be blank" }
    }

    public companion object {
        public val AiApiKey: SecureStorageKey = SecureStorageKey("ai.api_key")
        public val TtsApiKey: SecureStorageKey = SecureStorageKey("tts.api_key")
    }
}
