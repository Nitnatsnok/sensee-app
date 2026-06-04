package app.sensee.verification.core.hierarchy

import app.sensee.verification.core.contract.LexicalEntryTypeHint
import app.sensee.verification.core.contract.LexicalSourceRef
import app.sensee.verification.core.contract.PartOfSpeechHint
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Canonical-form-based id of a lemma (`en:come`). Stable across providers —
 * provider-specific ids ride along inside the [LexicalSourceRef] of each
 * observation, so two providers that share the same canonical form share
 * this id and merge cleanly.
 */
@Serializable
@JvmInline
public value class LemmaId(
    public val value: String,
) {
    public companion object {
        public fun of(
            language: String,
            canonical: String,
        ): LemmaId = LemmaId("$language:${canonical.trim().lowercase()}")
    }
}

/**
 * Canonical-form-based id of a unit (`en:come_across:phrasal_verb`). The
 * entry-type segment matters: `bear` the noun (animal) and `bear` the verb
 * (carry) are distinct units sharing no SRS state — and the family of `come`
 * holds `come (word)` and `come across (phrasal_verb)` as siblings.
 */
@Serializable
@JvmInline
public value class LexicalUnitId(
    public val value: String,
) {
    public companion object {
        public fun of(
            language: String,
            displayForm: String,
            entryType: LexicalEntryTypeHint,
        ): LexicalUnitId =
            LexicalUnitId(
                "$language:${displayForm.trim().lowercase().replace(' ', '_')}:${entryType.id}",
            )
    }
}

@Serializable
public data class LexicalLemma(
    val id: LemmaId,
    val canonical: String,
    val language: String,
    val pos: PartOfSpeechHint? = null,
    val sources: List<LexicalSourceRef> = emptyList(),
)

@Serializable
public data class LexicalUnitInfo(
    val id: LexicalUnitId,
    val displayForm: String,
    val entryType: LexicalEntryTypeHint,
    val headLemma: LemmaId? = null,
    val components: List<UnitComponentFact> = emptyList(),
    val sources: List<LexicalSourceRef> = emptyList(),
)

/**
 * Neutral mirror of a `UnitComponent`. The seam carries the role as a wire
 * id so the feature boundary can resolve it through `ComponentRole.fromId`.
 *
 * Intentional boundary mirror of `ai.core.UnitComponentFact`: both are
 * zero-dependency leaves with no shared ancestor; a common type would force a
 * shared dependency onto the deliberately dependency-free `ai.core` and
 * `verification.core` boundaries, so the duplication is by design (plan §3).
 */
@Serializable
public data class UnitComponentFact(
    val text: String,
    val role: String,
)
