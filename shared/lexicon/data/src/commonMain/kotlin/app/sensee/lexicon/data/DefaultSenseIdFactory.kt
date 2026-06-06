package app.sensee.lexicon.data

import app.sensee.lexicon.domain.SenseId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<SenseIdFactory>(),
)
@Inject
public class DefaultSenseIdFactory : SenseIdFactory {
    @OptIn(ExperimentalUuidApi::class)
    override fun mintPersonal(): SenseId = SenseId(Uuid.random().toString())

    override fun forServiceSource(sourceRef: String): SenseId = SenseId(SERVICE_PREFIX + fnv1aHex(sourceRef))

    private companion object {
        const val SERVICE_PREFIX = "svc-"

        // FNV-1a 64-bit → hex: deterministic on every target and colon-free, so a
        // Service source_ref maps to the same SenseId across re-syncs without a
        // crypto dependency. The id only has to be stable, not cryptographic: the
        // partial unique index keeps one row per source_ref, and at catalog scale a
        // 64-bit hash collision (two source_refs → one id) is negligible.
        fun fnv1aHex(value: String): String {
            var hash = 0xcbf29ce484222325uL
            for (byte in value.encodeToByteArray()) {
                hash = (hash xor byte.toUByte().toULong()) * 0x100000001b3uL
            }
            return hash.toString(radix = 16)
        }
    }
}
