package app.sensee.core.tracing.internal

import kotlin.random.Random
import kotlin.time.Clock

internal fun Clock.epochNanosNow(): Long {
    val now = now()
    return now.epochSeconds * 1_000_000_000L + now.nanosecondsOfSecond
}

internal fun randomTraceId(): String = randomHex(byteCount = 16)

internal fun randomSpanId(): String = randomHex(byteCount = 8)

private val hexChars = "0123456789abcdef".toCharArray()

private fun randomHex(byteCount: Int): String {
    val bytes = Random.nextBytes(byteCount)
    val sb = StringBuilder(byteCount * 2)
    for (b in bytes) {
        val ub = b.toInt() and 0xFF
        sb.append(hexChars[ub ushr 4])
        sb.append(hexChars[ub and 0x0F])
    }
    return sb.toString()
}
