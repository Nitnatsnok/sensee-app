package app.sensee.tts.cache

public data class AudioCacheConfig(
    val maxBytes: Long = DEFAULT_MAX_BYTES,
    val maxEntries: Long = DEFAULT_MAX_ENTRIES,
) {
    init {
        require(maxBytes > 0) { "maxBytes must be positive" }
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    public companion object {
        /** ~50 MB — comfortable for a few thousand short clips on mobile. */
        public const val DEFAULT_MAX_BYTES: Long = 50L * 1024 * 1024

        public const val DEFAULT_MAX_ENTRIES: Long = 5_000L
    }
}
