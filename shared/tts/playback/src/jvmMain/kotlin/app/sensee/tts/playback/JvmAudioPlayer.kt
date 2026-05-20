package app.sensee.tts.playback

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.AudioClip
import app.sensee.tts.core.TtsError
import app.sensee.tts.core.TtsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.coroutines.CoroutineContext

private const val BUFFER_SIZE = 4096
private const val SNIFF_BUFFER_SIZE = 64 * 1024
private const val FALLBACK_SAMPLE_RATE = 44_100f
private const val PCM_BITS = 16

/**
 * Plays audio through `javax.sound.sampled`. The synthesizers emit MP3, which
 * the JDK cannot decode on its own — the `mp3spi` service provider on the
 * runtime classpath registers the MP3 → PCM conversion that [AudioSystem] picks
 * up transparently here.
 *
 * [playStream] feeds the decoder as chunks arrive, so playback starts on the
 * first bytes instead of waiting for the whole clip to download.
 */
internal class JvmAudioPlayer(
    logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher,
) : AudioPlayer {
    private val log = logger.tag("JvmAudioPlayer")

    @Volatile
    private var line: SourceDataLine? = null

    @Volatile
    private var feed: ChunkFeedInputStream? = null

    @Volatile
    private var stopped = false

    override suspend fun play(
        clip: AudioClip,
        rateMultiplier: Float,
    ) {
        stopped = false
        withContext(ioDispatcher) {
            playPcmBlocking(
                pcm = decodeToPcm(ByteArrayInputStream(clip.bytes)),
                rateMultiplier = rateMultiplier,
                coroutineContext = currentCoroutineContext(),
            )
        }
    }

    override suspend fun playStream(
        chunks: Flow<ByteArray>,
        rateMultiplier: Float,
    ) {
        stopped = false
        withContext(ioDispatcher) {
            val source = ChunkFeedInputStream().also { feed = it }
            try {
                coroutineScope {
                    launch {
                        var completed = false
                        try {
                            chunks.collect { source.feed(it) }
                            source.finish()
                            completed = true
                            log.verbose { "stream producer finished" }
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (failure: Throwable) {
                            source.fail(failure)
                            log.verbose(failure) { "stream producer failed: ${failure.message ?: "no message"}" }
                            throw failure
                        } finally {
                            if (!completed) {
                                source.close()
                            }
                        }
                    }
                    playPcmBlocking(
                        pcm = decodeToPcm(BufferedInputStream(source, SNIFF_BUFFER_SIZE)),
                        rateMultiplier = rateMultiplier,
                        coroutineContext = currentCoroutineContext(),
                    )
                }
            } finally {
                feed = null
            }
        }
    }

    private fun playPcmBlocking(
        pcm: AudioInputStream,
        rateMultiplier: Float,
        coroutineContext: CoroutineContext,
    ) {
        log.debug {
            "decoded → PCM ${pcm.format.sampleRate.toInt()}Hz " +
                "${pcm.format.channels}ch, rate x$rateMultiplier"
        }
        val dataLine = openLine(pcm.format, rateMultiplier)
        line = dataLine
        log.debug { "audio line opened @ ${dataLine.format}" }
        try {
            dataLine.start()
            val buffer = ByteArray(BUFFER_SIZE)
            while (!stopped) {
                coroutineContext.ensureActive()
                val read = pcm.read(buffer, 0, buffer.size)
                if (read < 0) break
                dataLine.write(buffer, 0, read)
            }
            if (stopped) {
                log.debug { "playback stopped before completion" }
            } else {
                dataLine.drain()
                log.debug { "playback drained to completion" }
            }
        } catch (io: IOException) {
            throw TtsException(TtsError.Unknown(io.message), io)
        } finally {
            ignoreCleanupFailure { dataLine.stop() }
            ignoreCleanupFailure { dataLine.close() }
            ignoreCleanupFailure { pcm.close() }
            if (line === dataLine) line = null
        }
    }

    override fun stop() {
        stopped = true
        feed?.let { activeFeed -> ignoreCleanupFailure { activeFeed.close() } }
        line?.let { active ->
            ignoreCleanupFailure {
                active.stop()
                active.flush()
            }
        }
    }

    override fun release(): Unit = stop()
}

/**
 * Blocking [InputStream] fed by a producer coroutine. A bounded queue carries
 * chunks across coroutines without the same-thread restriction of
 * `PipedInputStream`; a sentinel marks end-of-stream and a captured failure is
 * rethrown to the reader so a streaming error surfaces instead of a silent EOF.
 */
internal class ChunkFeedInputStream : InputStream() {
    private val queue = LinkedBlockingQueue<ByteArray>()
    private val endOfStream = ByteArray(0)

    @Volatile
    private var failure: Throwable? = null

    @Volatile
    private var closed = false

    private var current: ByteArray = ByteArray(0)
    private var position = 0

    fun feed(bytes: ByteArray) {
        if (!closed && bytes.isNotEmpty()) queue.put(bytes)
    }

    fun finish() {
        queue.put(endOfStream)
    }

    fun fail(cause: Throwable) {
        failure = cause
        queue.put(endOfStream)
    }

    override fun read(): Int {
        val one = ByteArray(1)
        val n = read(one, 0, 1)
        return if (n < 0) -1 else one[0].toInt() and 0xFF
    }

    override fun read(
        destination: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        if (length == 0) return 0
        while (position >= current.size) {
            failure?.let { throw IOException("Audio stream failed", it) }
            if (current === endOfStream) return -1
            current = queue.take()
            position = 0
        }
        val available = current.size - position
        val toCopy = minOf(length, available)
        System.arraycopy(current, position, destination, offset, toCopy)
        position += toCopy
        return toCopy
    }

    override fun close() {
        closed = true
        queue.put(endOfStream)
    }
}

private fun ignoreCleanupFailure(block: () -> Unit) {
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
    }
}

private fun openLine(
    pcmFormat: AudioFormat,
    rateMultiplier: Float,
): SourceDataLine {
    val scaled =
        if (rateMultiplier != 1.0f) {
            AudioFormat(
                pcmFormat.sampleRate * rateMultiplier,
                pcmFormat.sampleSizeInBits,
                pcmFormat.channels,
                true,
                pcmFormat.isBigEndian,
            )
        } else {
            pcmFormat
        }
    return try {
        openLine(scaled)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        // A mixer may reject the rate-scaled format; fall back to plain speed.
        try {
            openLine(pcmFormat)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            throw TtsException(
                TtsError.Unknown("Audio line unavailable: ${failure.message ?: "no message"}"),
                failure,
            )
        }
    }
}

private fun openLine(format: AudioFormat): SourceDataLine {
    val info = DataLine.Info(SourceDataLine::class.java, format)
    val sourceLine = AudioSystem.getLine(info) as SourceDataLine
    sourceLine.open(format)
    return sourceLine
}

/**
 * Decodes [input] into a 16-bit signed PCM stream. [AudioSystem] sniffs the
 * container (MP3 via the `mp3spi` SPI, or WAV); unsupported or corrupt bytes
 * surface as a [TtsException] instead of an opaque sound API error.
 */
internal fun decodeToPcm(input: InputStream): AudioInputStream {
    val source =
        try {
            AudioSystem.getAudioInputStream(input)
        } catch (unsupported: UnsupportedAudioFileException) {
            throw TtsException(
                TtsError.Unknown("Unsupported audio data: ${unsupported.message ?: "no message"}"),
                unsupported,
            )
        } catch (io: IOException) {
            throw TtsException(
                TtsError.Unknown("Unreadable audio data: ${io.message ?: "no message"}"),
                io,
            )
        }
    val base = source.format
    val sampleRate = base.sampleRate.takeIf { it > 0f } ?: FALLBACK_SAMPLE_RATE
    val channels = base.channels.takeIf { it > 0 } ?: 1
    val frameSize = channels * (PCM_BITS / Byte.SIZE_BITS)
    val pcmFormat =
        AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            PCM_BITS,
            channels,
            frameSize,
            sampleRate,
            false,
        )
    return AudioSystem.getAudioInputStream(pcmFormat, source)
}

internal class JvmAudioPlayerFactory(
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher,
) : AudioPlayerFactory {
    override fun create(): AudioPlayer = JvmAudioPlayer(logger, ioDispatcher)
}

public actual fun audioPlayerFactory(
    context: PlatformContext,
    logger: AppLogger,
    dispatchers: AppDispatchers,
): AudioPlayerFactory = JvmAudioPlayerFactory(logger, dispatchers.io)
