package app.sensee.verification.core.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Identity, attribution and license of one verification source. Every
 * observation carries a [LexicalSourceRef] back to the source so the
 * orchestrator can deny content based on license — whether a snippet may be
 * passed to a third-party LLM ([LicensePolicy.usableAsLlmContext]) and whether
 * a response may be persisted to the on-device cache
 * ([LicensePolicy.storeContentAllowed]). Defaults are the most restrictive; an
 * adapter opts in deliberately.
 */
@Serializable
public sealed interface LexicalSource {
    public val id: String
    public val displayName: String
    public val attribution: AttributionPolicy
    public val license: LicensePolicy

    @Serializable
    @SerialName("adapter")
    public data class Adapter(
        override val id: String,
        override val displayName: String,
        override val attribution: AttributionPolicy,
        override val license: LicensePolicy,
        val versionTag: String? = null,
    ) : LexicalSource
}

/**
 * A pointer back to one specific entry/sense in a source. The seam stores
 * pointers only; full content (definitions, examples) is fetched on demand
 * by the orchestrator subject to [LicensePolicy.storeContentAllowed].
 */
@Serializable
public data class LexicalSourceRef(
    val sourceId: String,
    val entryId: String? = null,
    val senseId: String? = null,
    val url: String? = null,
    val fetchedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long? = null,
)

@Serializable
public data class AttributionPolicy(
    val required: Boolean,
    val displayString: String? = null,
    val mustLinkBack: Boolean = false,
    val licenseFile: String? = null,
)

/**
 * What the orchestrator is allowed to do with this source's content. Defaults
 * are the strictest possible — an adapter opts in explicitly per flag so a
 * misconfigured one is safe, not permissive.
 */
@Serializable
public data class LicensePolicy(
    val storeContentAllowed: Boolean = false,
    val maxCacheTtlMillis: Long? = null,
    val usableAsLlmContext: Boolean = false,
    val noteIfAny: String? = null,
)
