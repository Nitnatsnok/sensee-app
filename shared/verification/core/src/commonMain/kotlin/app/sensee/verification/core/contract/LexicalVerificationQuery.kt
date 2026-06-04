package app.sensee.verification.core.contract

import kotlinx.serialization.Serializable

/**
 * Input for one verification round. Provider-agnostic and feature-agnostic:
 * the seam never sees vendor types or feature domain types.
 *
 * [expectedEntryType] is load-bearing for multi-word units: an adapter looks
 * up `come across` as a phrasal verb, not as the sequence "come" + "across".
 * When the orchestrator does not know the entry type yet, leaving it `null`
 * lets the verifier guess from the surface form.
 */
@Serializable
public data class LexicalVerificationQuery(
    val text: String,
    val studyLanguageTag: String,
    val nativeLanguageTag: String? = null,
    val expectedEntryType: LexicalEntryTypeHint? = null,
    val expectedPartOfSpeech: PartOfSpeechHint? = null,
    val policy: VerificationPolicy = VerificationPolicy.Default,
)

/** Neutral POS tag. Resolved to feature taxonomy at the feature boundary. */
@Serializable
public data class PartOfSpeechHint(
    val id: String,
)

@Serializable
public data class VerificationPolicy(
    val timeoutPerProviderMillis: Long = DEFAULT_PROVIDER_TIMEOUT_MILLIS,
    val allowNetwork: Boolean = true,
    val maxCacheAgeMillis: Long? = null,
    val includeFamily: Boolean = false,
    val familySiblingCap: Int = DEFAULT_FAMILY_SIBLING_CAP,
) {
    public companion object {
        public const val DEFAULT_PROVIDER_TIMEOUT_MILLIS: Long = 4_000L
        public const val DEFAULT_FAMILY_SIBLING_CAP: Int = 20
        public val Default: VerificationPolicy = VerificationPolicy()
    }
}
