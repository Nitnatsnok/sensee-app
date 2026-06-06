package app.sensee.lexicon.data

import kotlin.math.sqrt

private const val BYTES_PER_FLOAT = 4
private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8

/**
 * Pack a float vector little-endian into bytes (4 per value) for the
 * `sense_embedding.vector` BLOB. Multiplatform by construction — bit-twiddling
 * via [Float.toRawBits], not `java.nio.ByteBuffer` — so it round-trips the same
 * on JVM, Android, Native, JS, and Wasm.
 */
internal fun FloatArray.toVectorBytes(): ByteArray {
    val bytes = ByteArray(size * BYTES_PER_FLOAT)
    for (i in indices) {
        var bits = this[i].toRawBits()
        val base = i * BYTES_PER_FLOAT
        for (offset in 0 until BYTES_PER_FLOAT) {
            bytes[base + offset] = (bits and BYTE_MASK).toByte()
            bits = bits ushr BITS_PER_BYTE
        }
    }
    return bytes
}

/** Inverse of [toVectorBytes]; the blob length must be a whole number of floats. */
internal fun ByteArray.toFloatVector(): FloatArray {
    require(size % BYTES_PER_FLOAT == 0) {
        "Vector blob size $size is not a multiple of $BYTES_PER_FLOAT"
    }
    val floats = FloatArray(size / BYTES_PER_FLOAT)
    for (i in floats.indices) {
        val base = i * BYTES_PER_FLOAT
        var bits = 0
        for (offset in BYTES_PER_FLOAT - 1 downTo 0) {
            bits = (bits shl BITS_PER_BYTE) or (this[base + offset].toInt() and BYTE_MASK)
        }
        floats[i] = Float.fromBits(bits)
    }
    return floats
}

/**
 * Scale to unit L2 length so cosine similarity reduces to a dot product and
 * stored vectors are comparable regardless of provider magnitude. Null for an
 * all-zero vector — it has no direction and cannot be normalized.
 */
internal fun FloatArray.l2Normalized(): FloatArray? {
    var norm = 0.0
    for (value in this) norm += value.toDouble() * value
    if (norm == 0.0) return null
    val inverse = (1.0 / sqrt(norm)).toFloat()
    return FloatArray(size) { this[it] * inverse }
}
