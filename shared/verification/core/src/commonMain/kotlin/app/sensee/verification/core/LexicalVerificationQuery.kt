package app.sensee.verification.core

import kotlinx.serialization.Serializable

/**
 * Input for one verification round. Provider-agnostic and feature-agnostic:
 * the seam never sees vendor types or feature domain types. AI-proposed
 * material rides in as `senseHints` and `examplesToValidate` — hints, NOT
 * ground truth: the verifier may corroborate, contradict, or stay silent on
 * each.
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
    val senseHints: List<SenseHint> = emptyList(),
    val examplesToValidate: List<ExampleHint> = emptyList(),
    val policy: VerificationPolicy = VerificationPolicy.Default,
)

/**
 * One sense the orchestrator already has (typically AI-generated). The
 * verifier may map it onto a dictionary sense, mark it as unmatched, or stay
 * silent if it does not handle sense inventory.
 */
@Serializable
public data class SenseHint(
    val id: String,
    val definition: String? = null,
    val translation: String? = null,
    val pos: PartOfSpeechHint? = null,
    val example: String? = null,
)

/**
 * One example sentence to validate. The structured [SentenceHint] carries the
 * studied unit's Target segments; an example-quality adapter that needs a
 * flat string flattens via [SentenceHint.plainText] internally and maps
 * findings back onto the segment topology before returning them.
 */
@Serializable
public data class ExampleHint(
    val sentence: SentenceHint,
    val senseHintId: String? = null,
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
