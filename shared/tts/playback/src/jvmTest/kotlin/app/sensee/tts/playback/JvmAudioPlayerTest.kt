package app.sensee.tts.playback

import app.sensee.tts.core.TtsException
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JvmAudioPlayerTest {
    @Test
    fun `decodes a wav clip into 16-bit signed pcm`() {
        val pcm = decodeToPcm(ByteArrayInputStream(silentWavBytes()))

        assertEquals(AudioFormat.Encoding.PCM_SIGNED, pcm.format.encoding)
        assertEquals(16, pcm.format.sampleSizeInBits)
        assertTrue(pcm.format.sampleRate > 0f)
    }

    @Test
    fun `unsupported audio bytes raise a tts exception`() {
        assertFailsWith<TtsException> {
            decodeToPcm(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)))
        }
    }

    @Test
    fun `chunk-fed stream decodes once enough bytes have arrived`() {
        val wav = silentWavBytes()
        val feed = ChunkFeedInputStream()
        thread {
            feed.feed(wav.copyOfRange(0, wav.size / 2))
            feed.feed(wav.copyOfRange(wav.size / 2, wav.size))
            feed.finish()
        }

        val pcm = decodeToPcm(BufferedInputStream(feed, 64 * 1024))

        assertEquals(AudioFormat.Encoding.PCM_SIGNED, pcm.format.encoding)
    }

    @Test
    fun `a failed producer surfaces as a tts exception to the reader`() {
        val feed = ChunkFeedInputStream()
        feed.fail(IllegalStateException("network died"))

        assertFailsWith<TtsException> {
            decodeToPcm(BufferedInputStream(feed, 64 * 1024))
        }
    }

    private fun silentWavBytes(): ByteArray {
        val format = AudioFormat(8_000f, 16, 1, true, false)
        val frames = 800
        val pcm = ByteArray(frames * format.frameSize)
        val input = AudioInputStream(ByteArrayInputStream(pcm), format, frames.toLong())
        val out = ByteArrayOutputStream()
        AudioSystem.write(input, AudioFileFormat.Type.WAVE, out)
        return out.toByteArray()
    }
}
