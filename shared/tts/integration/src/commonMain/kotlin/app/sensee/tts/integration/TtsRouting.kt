package app.sensee.tts.integration

import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.TtsProvider
import app.sensee.tts.core.VoiceId

/**
 * Provider selection for TTS. ElevenLabs needs its provider chosen plus an API
 * key and a voice; OpenAI needs its provider chosen plus an API key (voices are
 * a fixed known set). Otherwise the always-available system speaker. Pure so the
 * decision is unit tested without constructing platform speakers or clients.
 */
internal fun shouldUseElevenLabs(ai: AiSettings): Boolean =
    ai.ttsProvider == TtsProvider.ElevenLabs &&
        !ai.ttsApiKey.isNullOrBlank() &&
        !ai.ttsVoiceId.isNullOrBlank()

internal fun shouldUseOpenAi(ai: AiSettings): Boolean =
    ai.ttsProvider == TtsProvider.OpenAi && !ai.ttsApiKey.isNullOrBlank()

/**
 * The voice to route with. [VoiceId] rejects blank, and not every route
 * predicate guards `ttsVoiceId` (OpenAI voices are a known set), so a blank or
 * absent configured voice falls back to the request's own voice instead of
 * constructing an invalid [VoiceId] — `speak` must not throw across the seam.
 */
internal fun routedVoiceId(
    ai: AiSettings,
    requestVoiceId: VoiceId?,
): VoiceId? = ai.ttsVoiceId?.takeIf { it.isNotBlank() }?.let(::VoiceId) ?: requestVoiceId

internal fun routedModelId(
    ai: AiSettings,
    requestModelId: String?,
): String? = ai.ttsModel?.trim()?.takeIf { it.isNotEmpty() } ?: requestModelId?.takeIf { it.isNotBlank() }
