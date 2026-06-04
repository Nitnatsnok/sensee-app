package app.sensee.verification.core.contract

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * `kotlinx.serialization` does not handle [IntRange] out of the box. The
 * persisted-cache path needs ranges in [ExampleLocation], so we round-trip
 * via a small `(first, last)` surrogate.
 */
internal object IntRangeSerializer : KSerializer<IntRange> {
    @Serializable
    private data class Surrogate(
        val first: Int,
        val last: Int,
    )

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: IntRange,
    ) {
        Surrogate.serializer().serialize(encoder, Surrogate(value.first, value.last))
    }

    override fun deserialize(decoder: Decoder): IntRange {
        val surrogate = Surrogate.serializer().deserialize(decoder)
        return surrogate.first..surrogate.last
    }
}
