package app.sensee.tts.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpeechRequestTest {
    @Test
    fun `blank text is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            SpeechRequest(text = "  ", locale = SpeechLocale.English)
        }
    }

    @Test
    fun `rate bounds are enforced`() {
        assertFailsWith<IllegalArgumentException> { SpeechRate(0.1f) }
        assertFailsWith<IllegalArgumentException> { SpeechRate(3.0f) }
    }

    @Test
    fun `defaults are fast quality and normal rate`() {
        val request = SpeechRequest(text = "hello", locale = SpeechLocale.English)
        assertEquals(SpeechQuality.Fast, request.quality)
        assertEquals(SpeechRate.Normal, request.rate)
    }
}
