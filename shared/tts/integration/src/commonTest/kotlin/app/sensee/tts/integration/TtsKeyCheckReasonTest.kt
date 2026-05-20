package app.sensee.tts.integration

import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TtsKeyCheckReasonTest {
    @Test
    fun `a 401 or 403 is reported as a rejected key`() {
        val rejected = "API key was rejected by the provider"
        assertEquals(rejected, ttsKeyCheckReason(TtsException(TtsError.Network("nope", statusCode = 401))))
        assertEquals(rejected, ttsKeyCheckReason(TtsException(TtsError.Network("nope", statusCode = 403))))
    }

    @Test
    fun `a quota error is reported as quota exceeded`() {
        assertEquals(
            "ElevenLabs quota exceeded",
            ttsKeyCheckReason(TtsException(TtsError.Quota())),
        )
    }

    @Test
    fun `any other failure is reported as unreachable and never crashes`() {
        assertTrue(
            ttsKeyCheckReason(TtsException(TtsError.Network("boom", statusCode = 500)))
                .startsWith("Provider unreachable"),
        )
        assertTrue(
            ttsKeyCheckReason(RuntimeException("socket closed")).startsWith("Provider unreachable"),
        )
    }
}
