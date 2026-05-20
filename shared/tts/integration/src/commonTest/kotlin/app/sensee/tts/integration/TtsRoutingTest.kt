package app.sensee.tts.integration

import app.sensee.settings.domain.AiSettings
import app.sensee.settings.domain.TtsProvider
import app.sensee.tts.core.VoiceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TtsRoutingTest {
    @Test
    fun `elevenlabs requires its provider plus a key and a voice`() {
        assertTrue(
            shouldUseElevenLabs(
                AiSettings(
                    ttsProvider = TtsProvider.ElevenLabs,
                    ttsApiKey = "el-key",
                    ttsVoiceId = "voice-1",
                ),
            ),
        )
    }

    @Test
    fun `elevenlabs without a voice falls back to the system speaker`() {
        assertFalse(
            shouldUseElevenLabs(
                AiSettings(ttsProvider = TtsProvider.ElevenLabs, ttsApiKey = "el-key"),
            ),
        )
    }

    @Test
    fun `a key for the other provider does not select elevenlabs`() {
        assertFalse(
            shouldUseElevenLabs(
                AiSettings(
                    ttsProvider = TtsProvider.OpenAi,
                    ttsApiKey = "sk-key",
                    ttsVoiceId = "alloy",
                ),
            ),
        )
    }

    @Test
    fun `a blank configured voice does not build an invalid VoiceId - keeps the request voice`() {
        val requestVoice = VoiceId("request-voice")

        assertEquals(
            requestVoice,
            routedVoiceId(
                AiSettings(ttsProvider = TtsProvider.OpenAi, ttsApiKey = "sk-key", ttsVoiceId = "   "),
                requestVoice,
            ),
            "blank ttsVoiceId must not throw via VoiceId; fall back to the request voice",
        )
        assertEquals(
            requestVoice,
            routedVoiceId(AiSettings(ttsProvider = TtsProvider.OpenAi, ttsApiKey = "sk-key"), requestVoice),
            "absent ttsVoiceId falls back to the request voice",
        )
    }

    @Test
    fun `a configured voice is used when present`() {
        assertEquals(
            VoiceId("rachel"),
            routedVoiceId(
                AiSettings(ttsProvider = TtsProvider.ElevenLabs, ttsApiKey = "el", ttsVoiceId = "rachel"),
                VoiceId("request-voice"),
            ),
        )
    }

    @Test
    fun `a configured model is used when present`() {
        assertEquals(
            "eleven_multilingual_v2",
            routedModelId(
                AiSettings(
                    ttsProvider = TtsProvider.ElevenLabs,
                    ttsApiKey = "el",
                    ttsModel = "  eleven_multilingual_v2  ",
                ),
                "request-model",
            ),
        )
    }

    @Test
    fun `a blank configured model keeps the request model`() {
        assertEquals(
            "request-model",
            routedModelId(
                AiSettings(ttsProvider = TtsProvider.OpenAi, ttsApiKey = "sk-key", ttsModel = "   "),
                "request-model",
            ),
        )
        assertEquals(
            null,
            routedModelId(AiSettings(ttsProvider = TtsProvider.OpenAi, ttsApiKey = "sk-key"), "   "),
        )
    }

    @Test
    fun `openai requires its provider plus a key - voices are a known set`() {
        assertTrue(
            shouldUseOpenAi(AiSettings(ttsProvider = TtsProvider.OpenAi, ttsApiKey = "sk-key")),
        )
        assertFalse(shouldUseOpenAi(AiSettings(ttsProvider = TtsProvider.OpenAi, ttsApiKey = "  ")))
        assertFalse(
            shouldUseOpenAi(AiSettings(ttsProvider = TtsProvider.ElevenLabs, ttsApiKey = "sk-key")),
        )
    }
}
