package app.sensee.tts.playback

import app.sensee.tts.core.AudioFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioMimeTypeTest {
    @Test
    fun `mp3 maps to the audio mpeg media type`() {
        assertEquals("audio/mpeg", audioMimeType(AudioFormat.Mp3))
    }

    @Test
    fun `wav maps to the audio wav media type`() {
        assertEquals("audio/wav", audioMimeType(AudioFormat.Wav))
    }

    @Test
    fun `pcm16 maps to a playable wave media type`() {
        assertEquals("audio/wave", audioMimeType(AudioFormat.Pcm16))
    }
}
