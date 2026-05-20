package app.sensee.tts.cache

import app.sensee.tts.core.SpeechRequest

/**
 * Stable cache key derived from request content. Excludes [SpeechRequest.rate]
 * because rate is applied at playback time, not at synthesis — the same audio
 * bytes can be replayed at multiple rates.
 *
 * Encoded with explicit `` (unit separator) between fields so that no
 * combination of user text can collide with a different field configuration.
 */
internal fun SpeechRequest.toCacheKey(engineId: String): String =
    buildString {
        append(engineId).append('')
        append(locale.bcp47).append('')
        append(voiceId?.value ?: "_default_").append('')
        append(modelId?.takeIf { it.isNotBlank() } ?: "_default_").append('')
        append(quality.name).append('')
        append(text)
    }
